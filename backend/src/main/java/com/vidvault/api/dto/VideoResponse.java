package com.vidvault.api.dto;

import com.vidvault.api.domain.Video;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VideoResponse(
        UUID id,
        String title,
        String description,
        String category,
        int durationSeconds,
        long sizeBytes,
        String contentType,
        String status,
        long estimatedMonthlyViews,
        BigDecimal offerPrice,
        String offerBreakdown,
        UUID ownerId,
        String ownerDisplayName,
        boolean listedForSale,
        BigDecimal salePrice,
        BigDecimal aiFairPrice,
        String aiAnalysis,
        Instant createdAt,
        Instant resolvedAt) {

    public static VideoResponse from(Video v) {
        return new VideoResponse(
                v.getId(),
                v.getTitle(),
                v.getDescription(),
                v.getCategory(),
                v.getDurationSeconds(),
                v.getSizeBytes(),
                v.getContentType(),
                v.getStatus().name(),
                v.getEstimatedMonthlyViews(),
                v.getOfferPrice(),
                v.getOfferBreakdown(),
                v.getOwner().getId(),
                v.getOwner().getDisplayName(),
                v.isListedForSale(),
                v.getSalePrice(),
                v.getAiFairPrice(),
                v.getAiAnalysis(),
                v.getCreatedAt(),
                v.getResolvedAt());
    }
}
