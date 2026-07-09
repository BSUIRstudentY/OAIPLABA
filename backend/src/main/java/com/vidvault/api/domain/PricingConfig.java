package com.vidvault.api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Singleton (single row) configuration for the buyout pricing model.
 * Editable by administrators.
 */
@Entity
@Table(name = "pricing_config")
@Getter
@Setter
public class PricingConfig {

    @Id
    private Integer id = 1;

    /** Average CPM (revenue per 1000 monetised views), in USD. */
    @Column(name = "average_cpm", nullable = false, precision = 8, scale = 2)
    private BigDecimal averageCpm = new BigDecimal("2.50");

    /** Fraction of monetisable playbacks (watch-time / retention factor), 0..1. */
    @Column(name = "watch_time_factor", nullable = false, precision = 5, scale = 4)
    private BigDecimal watchTimeFactor = new BigDecimal("0.55");

    /** Number of months of projected revenue the platform pays for. */
    @Column(name = "projection_months", nullable = false)
    private int projectionMonths = 12;

    /** Share of projected revenue offered to the author, 0..1. */
    @Column(name = "buyout_share", nullable = false, precision = 5, scale = 4)
    private BigDecimal buyoutShare = new BigDecimal("0.40");

    /** Platform fee withheld from the offer, 0..1. */
    @Column(name = "platform_fee", nullable = false, precision = 5, scale = 4)
    private BigDecimal platformFee = new BigDecimal("0.10");

    /** Baseline monthly views assumed for a fresh upload before category weighting. */
    @Column(name = "base_monthly_views", nullable = false)
    private long baseMonthlyViews = 5000;
}
