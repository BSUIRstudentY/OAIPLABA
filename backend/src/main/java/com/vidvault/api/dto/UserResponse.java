package com.vidvault.api.dto;

import com.vidvault.api.domain.User;

import java.math.BigDecimal;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String role,
        BigDecimal walletBalance) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getWalletBalance());
    }
}
