package com.vidvault.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Transparent, step-by-step explanation of how a buyout offer was calculated,
 * so the author can see exactly how their YouTube monetisation potential maps
 * to the offered price.
 */
public record OfferBreakdown(
        String category,
        BigDecimal categoryMultiplier,
        long baseMonthlyViews,
        long estimatedMonthlyViews,
        BigDecimal averageCpm,
        BigDecimal watchTimeFactor,
        BigDecimal estimatedMonthlyRevenue,
        int projectionMonths,
        BigDecimal projectedRevenue,
        BigDecimal buyoutShare,
        BigDecimal grossOffer,
        BigDecimal platformFee,
        BigDecimal offerPrice,
        String currency,
        List<String> steps) {
}
