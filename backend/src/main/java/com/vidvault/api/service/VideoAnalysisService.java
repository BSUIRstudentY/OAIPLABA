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
    /** Maximum share of projected revenue paid for a top-quality video (content score = 1). */
    private static final double FAIR_SHARE = 0.50;

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

        // Monetisation anchor from the same pricing model used for platform offers
        // (realistic ~$0.01 per 1000 views, gated by duration).
        OfferBreakdown breakdown = pricingService.estimate(category, duration, expectedMonthlyViews);
        BigDecimal projectedRevenue = breakdown.projectedRevenue();

        // Content score in [0,1]: how much real, monetisable content the video carries.
        // Duration gates hard — a few-second clip with no editing is worth ~nothing,
        // no matter its resolution. Long + edited + with audio + HD approaches 1.0.
        double durationScore = durationScore(duration);
        double editingScore = editingScore(sceneTimes.size(), duration);
        double audioScore = probe.hasAudio ? 1.0 : 0.30;
        double resolutionScore = resolutionScore(probe);
        double qualityPart = 0.45 * editingScore + 0.30 * audioScore + 0.25 * resolutionScore;
        double contentScore = clamp(durationScore * qualityPart, 0.0, 1.0);

        // Pay a fair share of realistic projected revenue, scaled by content value.
        BigDecimal fairPrice = projectedRevenue
                .multiply(BigDecimal.valueOf(FAIR_SHARE))
                .multiply(BigDecimal.valueOf(contentScore))
                .setScale(2, RoundingMode.HALF_UP);

        String recommendation = recommendation(contentScore, fairPrice, duration);

        List<AiAnalysis.KeyMoment> keyMoments = buildKeyMoments(sceneTimes, duration);
        AiAnalysis.Technical technical = new AiAnalysis.Technical(
                probe.width, probe.height, resolutionLabel(probe.height), round1(probe.fps),
                probe.bitrateKbps, probe.hasAudio, sceneTimes.size(), duration);
        List<String> contextTags = buildContextTags(category, probe, duration, sceneTimes.size());
        List<AiAnalysis.Factor> factors = buildFactors(probe, duration, sceneTimes.size(),
                durationScore, editingScore, contentScore, breakdown);
        double confidence = confidence(probe, sceneTimes.size());
        String summary = buildSummary(fairPrice, breakdown, technical, contentScore, recommendation, keyMoments.size());
        String engine = probe.ok ? "content-analysis v1 (ffprobe + ffmpeg scene detection)"
                : "heuristic fallback (metadata only)";

        return new AiAnalysis(fairPrice, breakdown.currency(), confidence, contentScore, recommendation,
                summary, factors, keyMoments, contextTags, technical, engine);
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

    /**
     * How much the length alone justifies buying. Seconds-long clips score ~0
     * (not worth buying); value ramps up to a full 1.0 around the 5-minute mark.
     */
    private double durationScore(int durationSeconds) {
        if (durationSeconds < 30) return 0.0;      // too short to have real content
        return clamp((durationSeconds - 30) / (300.0 - 30.0), 0.0, 1.0);
    }

    /** Editing/montage effort inferred from scene changes. No cuts = raw, single-shot. */
    private double editingScore(int sceneChanges, int durationSeconds) {
        if (sceneChanges <= 0) return 0.15;
        double cutsPerMin = sceneChanges / Math.max(1.0, durationSeconds / 60.0);
        // Reward evidence of editing, saturating so frantic cutting isn't over-rewarded.
        return clamp(0.4 + Math.min(cutsPerMin, 6.0) * 0.10, 0.4, 1.0);
    }

    private double resolutionScore(Probe p) {
        if (!p.ok || p.height <= 0) return 0.5;
        if (p.height >= 1080) return 1.0;
        if (p.height >= 720) return 0.85;
        if (p.height >= 480) return 0.60;
        return 0.40;
    }

    private String recommendation(double contentScore, BigDecimal fairPrice, int durationSeconds) {
        if (durationSeconds < 30 || contentScore < 0.15 || fairPrice.doubleValue() < 0.05) {
            return "Not worth buying";
        }
        if (contentScore < 0.5) return "Low value";
        return "Worth buying";
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
                                                 double durationScore, double editingScore,
                                                 double contentScore, OfferBreakdown b) {
        List<AiAnalysis.Factor> f = new ArrayList<>();
        f.add(new AiAnalysis.Factor("Monetisation ceiling", "anchor",
                "At ~%s per 1000 views, projected YouTube revenue over %d months is only %s — the ceiling for any fair price."
                        .formatted(money(b.averageCpm(), b.currency()), b.projectionMonths(),
                                money(b.projectedRevenue(), b.currency()))));
        int minutes = duration / 60;
        f.add(new AiAnalysis.Factor("Length", durationScore >= 0.8 ? "+" : durationScore < 0.2 ? "-" : "=",
                duration < 30
                        ? "%d:%02d — far too short to carry real content or ads; almost no monetisation value."
                                .formatted(duration / 60, duration % 60)
                        : "%d:%02d runtime%s.".formatted(duration / 60, duration % 60,
                                minutes >= 4 && minutes <= 20 ? " — in the ad-friendly sweet spot" : "")));
        f.add(new AiAnalysis.Factor("Editing / montage", sceneChanges > 0 ? "+" : "-",
                sceneChanges == 0
                        ? "No scene changes detected — looks like a single raw shot with little editing effort."
                        : "%d scene change(s) — evidence of editing/montage (and the key-moment timecodes)."
                                .formatted(sceneChanges)));
        if (p.ok) {
            f.add(new AiAnalysis.Factor("Audio", p.hasAudio ? "+" : "-",
                    p.hasAudio ? "Has an audio track — needed to hold an audience." : "No audio — poor for viewer retention."));
            f.add(new AiAnalysis.Factor("Resolution", p.height >= 1080 ? "+" : p.height < 480 ? "-" : "=",
                    "%s source (%dx%d).".formatted(resolutionLabel(p.height), p.width, p.height)));
        } else {
            f.add(new AiAnalysis.Factor("Content probe", "-",
                    "Could not decode the media; valued from metadata only, so confidence is lower."));
        }
        f.add(new AiAnalysis.Factor("Content score", contentScore >= 0.5 ? "+" : "-",
                "Overall content value %.0f%% (length x editing/audio/quality). Fair price = %.0f%% of projected revenue x this score."
                        .formatted(contentScore * 100, FAIR_SHARE * 100)));
        return f;
    }

    private String buildSummary(BigDecimal fairPrice, OfferBreakdown b, AiAnalysis.Technical t,
                                double contentScore, String recommendation, int keyMomentCount) {
        String verdict;
        if (t.durationSeconds() < 30) {
            verdict = "This clip is only %d:%02d long — too short to carry real content, so it is essentially not worth buying."
                    .formatted(t.durationSeconds() / 60, t.durationSeconds() % 60);
        } else if (contentScore < 0.5) {
            verdict = "Limited content value (%.0f%%): short and/or lightly edited, so realistic YouTube earnings are low."
                    .formatted(contentScore * 100);
        } else {
            verdict = "Solid content value (%.0f%%): long enough, edited, and watchable — worth buying based on realistic earnings."
                    .formatted(contentScore * 100);
        }
        return ("AI fair value: %s (%s). %s The engine assumes ~%s per 1000 views, so projected revenue over %d months is %s; "
                + "it pays at most %.0f%% of that, scaled by content value. %d key moment(s) identified.")
                .formatted(
                        money(fairPrice, b.currency()), recommendation, verdict,
                        money(b.averageCpm(), b.currency()), b.projectionMonths(),
                        money(b.projectedRevenue(), b.currency()), FAIR_SHARE * 100, keyMomentCount);
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
