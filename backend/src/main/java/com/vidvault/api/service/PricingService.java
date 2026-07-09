package com.vidvault.api.service;

import com.vidvault.api.domain.PricingConfig;
import com.vidvault.api.dto.OfferBreakdown;
import com.vidvault.api.repo.PricingConfigRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estimates the buyout price of a video based on its projected YouTube
 * monetisation potential. The model is intentionally simple and fully
 * explainable; all knobs live in {@link PricingConfig} and are editable by
 * administrators.
 */
@Service
public class PricingService {

    private static final String CURRENCY = "USD";

    /** Per-category demand multipliers applied to the baseline monthly views. */
    private static final Map<String, BigDecimal> CATEGORY_MULTIPLIERS = new LinkedHashMap<>() {{
        put("education", new BigDecimal("1.30"));
        put("technology", new BigDecimal("1.40"));
        put("finance", new BigDecimal("1.60"));
        put("gaming", new BigDecimal("1.10"));
        put("music", new BigDecimal("1.20"));
        put("entertainment", new BigDecimal("1.00"));
        put("vlog", new BigDecimal("0.90"));
        put("sports", new BigDecimal("1.05"));
        put("other", new BigDecimal("0.80"));
    }};

    private final PricingConfigRepository pricingConfigRepository;

    public PricingService(PricingConfigRepository pricingConfigRepository) {
        this.pricingConfigRepository = pricingConfigRepository;
    }

    public List<String> categories() {
        return List.copyOf(CATEGORY_MULTIPLIERS.keySet());
    }

    public PricingConfig currentConfig() {
        return pricingConfigRepository.findById(1)
                .orElseThrow(() -> new IllegalStateException("Pricing config not initialised"));
    }

    private BigDecimal categoryMultiplier(String category) {
        return CATEGORY_MULTIPLIERS.getOrDefault(
                category == null ? "other" : category.toLowerCase(),
                CATEGORY_MULTIPLIERS.get("other"));
    }

    /**
     * Videos between 4 and 20 minutes earn a small watch-time bonus (they can
     * carry more mid-roll ads); very short or very long videos are discounted.
     */
    private BigDecimal durationFactor(int durationSeconds) {
        int minutes = Math.max(0, durationSeconds) / 60;
        if (minutes >= 4 && minutes <= 20) {
            return new BigDecimal("1.10");
        }
        if (minutes < 1) {
            return new BigDecimal("0.70");
        }
        return BigDecimal.ONE;
    }

    public OfferBreakdown estimate(String category, int durationSeconds, Long providedExpectedViews) {
        PricingConfig cfg = currentConfig();
        BigDecimal categoryMultiplier = categoryMultiplier(category);
        BigDecimal durationFactor = durationFactor(durationSeconds);

        long estimatedMonthlyViews;
        if (providedExpectedViews != null && providedExpectedViews > 0) {
            estimatedMonthlyViews = providedExpectedViews;
        } else {
            estimatedMonthlyViews = new BigDecimal(cfg.getBaseMonthlyViews())
                    .multiply(categoryMultiplier)
                    .multiply(durationFactor)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        }

        BigDecimal monthlyRevenue = new BigDecimal(estimatedMonthlyViews)
                .multiply(cfg.getAverageCpm())
                .divide(new BigDecimal(1000), 6, RoundingMode.HALF_UP)
                .multiply(cfg.getWatchTimeFactor())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal projectedRevenue = monthlyRevenue
                .multiply(new BigDecimal(cfg.getProjectionMonths()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal grossOffer = projectedRevenue
                .multiply(cfg.getBuyoutShare())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal offerPrice = grossOffer
                .multiply(BigDecimal.ONE.subtract(cfg.getPlatformFee()))
                .setScale(2, RoundingMode.HALF_UP);

        List<String> steps = List.of(
                "Estimated monthly views = %d".formatted(estimatedMonthlyViews),
                "Monthly revenue = views x CPM / 1000 x watch-time = %s".formatted(monthlyRevenue),
                "Projected revenue over %d months = %s".formatted(cfg.getProjectionMonths(), projectedRevenue),
                "Gross offer = projected x buyout share (%s) = %s".formatted(cfg.getBuyoutShare(), grossOffer),
                "Offer price = gross x (1 - platform fee %s) = %s %s"
                        .formatted(cfg.getPlatformFee(), offerPrice, CURRENCY));

        return new OfferBreakdown(
                category,
                categoryMultiplier,
                cfg.getBaseMonthlyViews(),
                estimatedMonthlyViews,
                cfg.getAverageCpm(),
                cfg.getWatchTimeFactor(),
                monthlyRevenue,
                cfg.getProjectionMonths(),
                projectedRevenue,
                cfg.getBuyoutShare(),
                grossOffer,
                cfg.getPlatformFee(),
                offerPrice,
                CURRENCY,
                steps);
    }
}
