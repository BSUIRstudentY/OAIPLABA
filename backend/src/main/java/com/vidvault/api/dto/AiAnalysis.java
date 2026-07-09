package com.vidvault.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of the AI valuation engine: a fair, quality-adjusted price for a video
 * together with a human-readable rationale, the technical signals it extracted,
 * and timecodes of the key moments detected in the content.
 */
public record AiAnalysis(
        BigDecimal fairPrice,
        String currency,
        /** 0..1 — how confident the engine is, based on how much real signal it could extract. */
        double confidence,
        String summary,
        List<Factor> factors,
        List<KeyMoment> keyMoments,
        List<String> contextTags,
        Technical technical,
        String engine) {

    /** A single pricing factor and how it influenced the fair price. */
    public record Factor(String name, String impact, String detail) {
    }

    /** A notable moment in the video, with a human-readable timecode. */
    public record KeyMoment(double timeSeconds, String timecode, String label) {
    }

    /** Technical signals extracted from the media. */
    public record Technical(
            int width,
            int height,
            String resolutionLabel,
            double fps,
            long bitrateKbps,
            boolean hasAudio,
            int sceneChanges,
            int durationSeconds) {
    }
}
