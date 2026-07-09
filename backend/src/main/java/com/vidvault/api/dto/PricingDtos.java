package com.vidvault.api.dto;

import com.vidvault.api.domain.PricingConfig;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class PricingDtos {

    private PricingDtos() {
    }

    public record PricingConfigResponse(
            BigDecimal averageCpm,
            BigDecimal watchTimeFactor,
            int projectionMonths,
            BigDecimal buyoutShare,
            BigDecimal platformFee,
            long baseMonthlyViews) {

        public static PricingConfigResponse from(PricingConfig c) {
            return new PricingConfigResponse(
                    c.getAverageCpm(),
                    c.getWatchTimeFactor(),
                    c.getProjectionMonths(),
                    c.getBuyoutShare(),
                    c.getPlatformFee(),
                    c.getBaseMonthlyViews());
        }
    }

    public record UpdatePricingRequest(
            @NotNull @DecimalMin("0.0") BigDecimal averageCpm,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal watchTimeFactor,
            @Min(1) int projectionMonths,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal buyoutShare,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal platformFee,
            @Min(0) long baseMonthlyViews) {
    }
}
