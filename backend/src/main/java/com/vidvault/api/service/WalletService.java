package com.vidvault.api.service;

import com.vidvault.api.domain.TransactionType;
import com.vidvault.api.domain.User;
import com.vidvault.api.domain.WalletTransaction;
import com.vidvault.api.dto.WalletDtos.TransactionResponse;
import com.vidvault.api.dto.WalletDtos.WalletResponse;
import com.vidvault.api.repo.UserRepository;
import com.vidvault.api.repo.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {

    private final UserRepository userRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletService(UserRepository userRepository, WalletTransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public WalletTransaction credit(User user, BigDecimal amount, TransactionType type,
                                    String description, UUID videoId) {
        User managed = userRepository.findById(user.getId()).orElseThrow();
        BigDecimal newBalance = managed.getWalletBalance().add(amount);
        managed.setWalletBalance(newBalance);
        userRepository.save(managed);

        WalletTransaction tx = new WalletTransaction();
        tx.setUser(managed);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setDescription(description);
        tx.setVideoId(videoId);
        return transactionRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<TransactionResponse> txs = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(TransactionResponse::from).toList();
        return new WalletResponse(user.getWalletBalance(), txs);
    }
}
