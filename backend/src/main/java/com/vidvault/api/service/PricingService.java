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
     * Duration strongly gates how many monetisable views a video can attract.
     * Very short clips (a few seconds) realistically earn almost nothing, so
     * they are gated close to zero; ad-friendly 4-20 minute videos reach the
     * full estimate. This prevents over-valuing short, low-effort uploads.
     */
    public static double durationViewFactor(int durationSeconds) {
        int s = Math.max(0, durationSeconds);
        if (s < 30) return 0.02;      // seconds-long clip: effectively worthless
        if (s < 60) return 0.10;      // < 1 min
        if (s < 240) return 0.50;     // 1-4 min
        if (s <= 1200) return 1.00;   // 4-20 min sweet spot
        return 0.85;                  // very long
    }

    public OfferBreakdown estimate(String category, int durationSeconds, Long providedExpectedViews) {
        PricingConfig cfg = currentConfig();
        BigDecimal categoryMultiplier = categoryMultiplier(category);
        BigDecimal durationFactor = BigDecimal.valueOf(durationViewFactor(durationSeconds));

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

        String viewsExplain = (providedExpectedViews != null && providedExpectedViews > 0)
                ? "Estimated monthly views = %d (provided by uploader)".formatted(estimatedMonthlyViews)
                : "Estimated monthly views = %d (base %d x category %s x duration gate %.2f)"
                        .formatted(estimatedMonthlyViews, cfg.getBaseMonthlyViews(), categoryMultiplier,
                                durationViewFactor(durationSeconds));
        List<String> steps = List.of(
                viewsExplain,
                "Monthly revenue = views x %s per 1000 views = %s".formatted(cfg.getAverageCpm(), monthlyRevenue),
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
