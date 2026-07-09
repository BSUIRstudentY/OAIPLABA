package com.vidvault.api.dto;

import com.vidvault.api.domain.WalletTransaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class WalletDtos {

    private WalletDtos() {
    }

    public record TransactionResponse(
            UUID id,
            String type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String description,
            UUID videoId,
            Instant createdAt) {

        public static TransactionResponse from(WalletTransaction tx) {
            return new TransactionResponse(
                    tx.getId(),
                    tx.getType().name(),
                    tx.getAmount(),
                    tx.getBalanceAfter(),
                    tx.getDescription(),
                    tx.getVideoId(),
                    tx.getCreatedAt());
        }
    }

    public record WalletResponse(
            BigDecimal balance,
            List<TransactionResponse> transactions) {
    }
}
