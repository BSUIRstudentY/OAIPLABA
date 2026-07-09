package com.vidvault.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class MarketplaceDtos {

    private MarketplaceDtos() {
    }

    public record ListRequest(
            @NotNull @DecimalMin(value = "0.01", message = "Price must be greater than zero") BigDecimal price) {
    }
}
