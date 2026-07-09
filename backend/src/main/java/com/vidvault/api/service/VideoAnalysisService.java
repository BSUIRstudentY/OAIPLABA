package com.vidvault.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidvault.api.dto.AiAnalysis;
import com.vidvault.api.dto.OfferBreakdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI valuation engine. It extracts real signals from the uploaded media with
 * ffprobe/ffmpeg (resolution, frame rate, bitrate, audio presence, and scene
 * changes for key-moment timecodes), derives a quality score, and produces a
 * deliberately conservative "fair price" anchored to the projected monetisation
 * revenue — so it never over-values a clip — together with an explanation of
 * every factor. The commentary generation is isolated so it can later be
 * swapped for an LLM without touching the valuation logic.
 */
@Service
public class VideoAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(VideoAnalysisService.class);
    private static final Pattern PTS_TIME = Pattern.compile("pts_time:([0-9]+\\.?[0-9]*)");

    private final PricingService pricingService;
    private final ObjectMapper objectMapper;
    private final String ffprobePath;
    private final String ffmpegPath;

    public VideoAnalysisService(PricingService pricingService, ObjectMapper objectMapper,
                                @Value("${vidvault.media.ffprobe-path:ffprobe}") String ffprobePath,
                                @Value("${vidvault.media.ffmpeg-path:ffmpeg}") String ffmpegPath) {
        this.pricingService = pricingService;
        this.objectMapper = objectMapper;
        this.ffprobePath = ffprobePath;
        this.ffmpegPath = ffmpegPath;
    }

    public AiAnalysis analyze(File file, String category, int fallbackDurationSeconds, Long expectedMonthlyViews) {
        Probe probe = probe(file);
        int duration = probe.durationSeconds > 0 ? probe.durationSeconds : Math.max(0, fallbackDurationSeconds);
        List<Double> sceneTimes = detectScenes(file, duration);

        // Monetisation anchor from the same pricing model used for platform offers.
        OfferBreakdown breakdown = pricingService.estimate(category, duration, expectedMonthlyViews);
        BigDecimal projectedRevenue = breakdown.projectedRevenue();

        double quality = qualityScore(probe, duration, sceneTimes.size());
        // Fair share of projected revenue, scaled by quality but capped to stay conservative.
        double fairShare = clamp(0.30 * quality, 0.15, 0.50);
        BigDecimal fairPrice = projectedRevenue
                .multiply(BigDecimal.valueOf(fairShare))
                .setScale(2, RoundingMode.HALF_UP);

        List<AiAnalysis.KeyMoment> keyMoments = buildKeyMoments(sceneTimes, duration);
        AiAnalysis.Technical technical = new AiAnalysis.Technical(
                probe.width, probe.height, resolutionLabel(probe.height), round1(probe.fps),
                probe.bitrateKbps, probe.hasAudio, sceneTimes.size(), duration);
        List<String> contextTags = buildContextTags(category, probe, duration, sceneTimes.size());
        List<AiAnalysis.Factor> factors = buildFactors(probe, duration, sceneTimes.size(), quality, fairShare, breakdown);
        double confidence = confidence(probe, sceneTimes.size());
        String summary = buildSummary(fairPrice, breakdown, technical, fairShare, quality, keyMoments.size());
        String engine = probe.ok ? "content-analysis v1 (ffprobe + ffmpeg scene detection)"
                : "heuristic fallback (metadata only)";

        return new AiAnalysis(fairPrice, breakdown.currency(), confidence, summary,
                factors, keyMoments, contextTags, technical, engine);
    }

    // ---- Signal extraction ----

    private record Probe(boolean ok, int width, int height, double fps, long bitrateKbps,
                         boolean hasAudio, int durationSeconds) {
    }

    private Probe probe(File file) {
        try {
            String json = run(List.of(ffprobePath, "-v", "quiet", "-print_format", "json",
                    "-show_format", "-show_streams", file.getAbsolutePath()), 30);
            JsonNode root = objectMapper.readTree(json);
            int width = 0, height = 0;
            double fps = 0;
            boolean hasAudio = false;
            for (JsonNode s : root.path("streams")) {
                String type = s.path("codec_type").asText("");
                if ("video".equals(type) && width == 0) {
                    width = s.path("width").asInt(0);
                    height = s.path("height").asInt(0);
                    fps = parseFraction(s.path("avg_frame_rate").asText("0/1"));
                } else if ("audio".equals(type)) {
                    hasAudio = true;
                }
            }
            JsonNode format = root.path("format");
            int duration = (int) Math.round(format.path("duration").asDouble(0));
            long bitrate = format.path("bit_rate").asLong(0) / 1000;
            return new Probe(true, width, height, fps, bitrate, hasAudio, duration);
        } catch (Exception e) {
            log.warn("ffprobe analysis failed ({}); falling back to metadata only", e.getMessage());
            return new Probe(false, 0, 0, 0, 0, false, 0);
        }
    }

    private List<Double> detectScenes(File file, int duration) {
        List<Double> times = new ArrayList<>();
        try {
            // Analyse at most the first 3 minutes to keep uploads responsive.
            String stderr = run(List.of(ffmpegPath, "-hide_banner", "-nostats", "-i", file.getAbsolutePath(),
                    "-t", "180", "-filter:v", "select='gt(scene,0.3)',showinfo",
                    "-f", "null", "-"), 60);
            Matcher m = PTS_TIME.matcher(stderr);
            while (m.find()) {
                double t = Double.parseDouble(m.group(1));
                if (duration <= 0 || t <= duration) {
                    times.add(t);
                }
            }
        } catch (Exception e) {
            log.debug("Scene detection failed: {}", e.getMessage());
        }
        return times;
    }

    /** Runs a process, returns combined stdout (ffprobe) or stderr (ffmpeg showinfo) text. */
    private String run(List<String> command, int timeoutSeconds) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        // ffmpeg writes showinfo to stderr; ffprobe writes JSON to stdout. Merge so callers get both.
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Media tool timed out");
        }
        return out.toString();
    }

    // ---- Scoring ----

    private double qualityScore(Probe p, int duration, int sceneChanges) {
        double score = 1.0;
        if (p.ok) {
            if (p.height >= 2160) score *= 1.20;
            else if (p.height >= 1080) score *= 1.15;
            else if (p.height >= 720) score *= 1.05;
            else if (p.height >= 480) score *= 0.95;
            else if (p.height > 0) score *= 0.80;

            score *= p.hasAudio ? 1.05 : 0.90;

            if (p.fps >= 50) score *= 1.05;
        }
        int minutes = duration / 60;
        if (minutes >= 4 && minutes <= 20) score *= 1.05;
        else if (duration > 0 && duration < 30) score *= 0.85;

        if (duration > 0) {
            double cutsPerMin = sceneChanges / Math.max(1.0, duration / 60.0);
            if (cutsPerMin >= 0.5 && cutsPerMin <= 8) score *= 1.05; // engaging pacing
            else if (sceneChanges == 0 && p.ok) score *= 0.95;       // static / single shot
        }
        return clamp(score, 0.60, 1.30);
    }

    private double confidence(Probe p, int sceneChanges) {
        double c = 0.40;
        if (p.ok) c += 0.30;
        if (sceneChanges > 0) c += 0.15;
        if (p.hasAudio) c += 0.05;
        if (p.width > 0) c += 0.05;
        return clamp(c, 0.30, 0.95);
    }

    // ---- Presentation ----

    private List<AiAnalysis.KeyMoment> buildKeyMoments(List<Double> sceneTimes, int duration) {
        List<AiAnalysis.KeyMoment> moments = new ArrayList<>();
        // Always include the opening hook.
        moments.add(new AiAnalysis.KeyMoment(0, timecode(0), "Opening / hook"));
        // Evenly sample up to 5 detected scene changes across the timeline.
        if (!sceneTimes.isEmpty()) {
            int want = Math.min(5, sceneTimes.size());
            int step = Math.max(1, sceneTimes.size() / want);
            int idx = 1;
            for (int i = 0; i < sceneTimes.size() && moments.size() <= want; i += step) {
                double t = sceneTimes.get(i);
                moments.add(new AiAnalysis.KeyMoment(t, timecode(t), "Scene change / key moment " + idx++));
            }
        } else if (duration >= 20) {
            // No scene data: mark the midpoint as a heuristic highlight.
            double mid = duration / 2.0;
            moments.add(new AiAnalysis.KeyMoment(mid, timecode(mid), "Midpoint highlight (estimated)"));
        }
        return moments;
    }

    private List<String> buildContextTags(String category, Probe p, int duration, int sceneChanges) {
        List<String> tags = new ArrayList<>();
        tags.add(category == null ? "other" : category.toLowerCase());
        if (p.ok) {
            tags.add(resolutionLabel(p.height));
            tags.add(p.hasAudio ? "with audio" : "silent");
        }
        int minutes = duration / 60;
        tags.add(minutes >= 4 && minutes <= 20 ? "ad-friendly length"
                : duration > 0 && duration < 60 ? "short-form" : "long-form");
        if (duration > 0) {
            double cutsPerMin = sceneChanges / Math.max(1.0, duration / 60.0);
            tags.add(cutsPerMin > 6 ? "fast-paced" : cutsPerMin < 0.5 ? "slow-paced" : "moderate pacing");
        }
        return tags;
    }

    private List<AiAnalysis.Factor> buildFactors(Probe p, int duration, int sceneChanges,
                                                 double quality, double fairShare, OfferBreakdown b) {
        List<AiAnalysis.Factor> f = new ArrayList<>();
        f.add(new AiAnalysis.Factor("Monetisation potential", "anchor",
                "Projected %s over %d months of YouTube monetisation is the ceiling for a fair price."
                        .formatted(money(b.projectedRevenue(), b.currency()), b.projectionMonths())));
        if (p.ok) {
            f.add(new AiAnalysis.Factor("Resolution", p.height >= 1080 ? "+" : p.height < 480 ? "-" : "=",
                    "%s source (%dx%d).".formatted(resolutionLabel(p.height), p.width, p.height)));
            f.add(new AiAnalysis.Factor("Audio", p.hasAudio ? "+" : "-",
                    p.hasAudio ? "Has an audio track — better for monetisation." : "No audio track detected."));
        } else {
            f.add(new AiAnalysis.Factor("Content probe", "-",
                    "Could not decode the media; valued from metadata only, so confidence is lower."));
        }
        int minutes = duration / 60;
        f.add(new AiAnalysis.Factor("Length", minutes >= 4 && minutes <= 20 ? "+" : duration < 30 ? "-" : "=",
                "%d:%02d runtime.".formatted(duration / 60, duration % 60)
                        + (minutes >= 4 && minutes <= 20 ? " In the ad-friendly sweet spot." : "")));
        f.add(new AiAnalysis.Factor("Pacing & structure", sceneChanges > 0 ? "+" : "=",
                sceneChanges + " scene change(s) detected — used for engagement and key-moment timecodes."));
        f.add(new AiAnalysis.Factor("Quality multiplier", quality >= 1.0 ? "+" : "-",
                "Overall quality factor x%.2f, so the fair price is %.0f%% of projected revenue (kept conservative)."
                        .formatted(quality, fairShare * 100)));
        return f;
    }

    private String buildSummary(BigDecimal fairPrice, OfferBreakdown b, AiAnalysis.Technical t,
                                double fairShare, double quality, int keyMomentCount) {
        return ("AI fair value: %s. This %s clip runs %d:%02d%s with %d key moment(s) identified. "
                + "The engine anchors on the projected %s of monetisation revenue and applies a quality "
                + "factor of x%.2f, pricing it at a conservative %.0f%% of that projection to avoid over-paying.")
                .formatted(
                        money(fairPrice, b.currency()),
                        t.resolutionLabel(),
                        t.durationSeconds() / 60, t.durationSeconds() % 60,
                        t.hasAudio() ? " with audio" : "",
                        keyMomentCount,
                        money(b.projectedRevenue(), b.currency()),
                        quality,
                        fairShare * 100);
    }

    // ---- helpers ----

    private static double parseFraction(String frac) {
        try {
            String[] parts = frac.split("/");
            double num = Double.parseDouble(parts[0]);
            double den = parts.length > 1 ? Double.parseDouble(parts[1]) : 1;
            return den == 0 ? 0 : num / den;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String resolutionLabel(int height) {
        if (height >= 2160) return "4K";
        if (height >= 1080) return "1080p";
        if (height >= 720) return "720p";
        if (height >= 480) return "480p";
        if (height > 0) return "SD";
        return "unknown";
    }

    private static String timecode(double seconds) {
        int s = (int) Math.floor(seconds);
        return "%d:%02d".formatted(s / 60, s % 60);
    }

    private static String money(BigDecimal v, String currency) {
        return "%s %s".formatted(v.setScale(2, RoundingMode.HALF_UP).toPlainString(), currency);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
