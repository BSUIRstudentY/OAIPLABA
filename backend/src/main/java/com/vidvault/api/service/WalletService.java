package com.vidvault.api.service;

import com.vidvault.api.domain.TransactionType;
import com.vidvault.api.domain.User;
import com.vidvault.api.domain.WalletTransaction;
import com.vidvault.api.dto.WalletDtos.TransactionResponse;
import com.vidvault.api.dto.WalletDtos.WalletResponse;
import com.vidvault.api.repo.UserRepository;
import com.vidvault.api.repo.WalletTransactionRepository;
import com.vidvault.api.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {

    private static final BigDecimal MAX_TXN = new BigDecimal("1000000");

    private final UserRepository userRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletService(UserRepository userRepository, WalletTransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    /** Adds funds to the user's wallet. {@code amount} must be positive. */
    @Transactional
    public WalletTransaction credit(User user, BigDecimal amount, TransactionType type,
                                    String description, UUID videoId) {
        BigDecimal amt = normalizePositive(amount);
        User managed = userRepository.findById(user.getId()).orElseThrow();
        BigDecimal newBalance = managed.getWalletBalance().add(amt);
        managed.setWalletBalance(newBalance);
        userRepository.save(managed);
        return record(managed, type, amt, newBalance, description, videoId);
    }

    /** Removes funds from the user's wallet, failing if the balance is insufficient. */
    @Transactional
    public WalletTransaction debit(User user, BigDecimal amount, TransactionType type,
                                   String description, UUID videoId) {
        BigDecimal amt = normalizePositive(amount);
        User managed = userRepository.findById(user.getId()).orElseThrow();
        if (managed.getWalletBalance().compareTo(amt) < 0) {
            throw ApiException.badRequest("Insufficient wallet balance");
        }
        BigDecimal newBalance = managed.getWalletBalance().subtract(amt);
        managed.setWalletBalance(newBalance);
        userRepository.save(managed);
        return record(managed, type, amt.negate(), newBalance, description, videoId);
    }

    @Transactional
    public WalletResponse deposit(User user, BigDecimal amount) {
        credit(user, amount, TransactionType.DEPOSIT, "Wallet top-up (simulated)", null);
        return getWallet(user.getId());
    }

    @Transactional
    public WalletResponse withdraw(User user, BigDecimal amount) {
        debit(user, amount, TransactionType.WITHDRAWAL, "Withdrawal (simulated)", null);
        return getWallet(user.getId());
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<TransactionResponse> txs = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(TransactionResponse::from).toList();
        return new WalletResponse(user.getWalletBalance(), txs);
    }

    private WalletTransaction record(User user, TransactionType type, BigDecimal signedAmount,
                                     BigDecimal balanceAfter, String description, UUID videoId) {
        WalletTransaction tx = new WalletTransaction();
        tx.setUser(user);
        tx.setType(type);
        tx.setAmount(signedAmount);
        tx.setBalanceAfter(balanceAfter);
        tx.setDescription(description);
        tx.setVideoId(videoId);
        return transactionRepository.save(tx);
    }

    private BigDecimal normalizePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw ApiException.badRequest("Amount must be greater than zero");
        }
        if (amount.compareTo(MAX_TXN) > 0) {
            throw ApiException.badRequest("Amount exceeds the allowed maximum");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
