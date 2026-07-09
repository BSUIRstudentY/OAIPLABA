package com.vidvault.api.web;

import com.vidvault.api.domain.User;
import com.vidvault.api.dto.WalletDtos.AmountRequest;
import com.vidvault.api.dto.WalletDtos.WalletResponse;
import com.vidvault.api.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public WalletResponse wallet(@AuthenticationPrincipal User user) {
        return walletService.getWallet(user.getId());
    }

    @PostMapping("/deposit")
    public WalletResponse deposit(@AuthenticationPrincipal User user, @Valid @RequestBody AmountRequest req) {
        return walletService.deposit(user, req.amount());
    }

    @PostMapping("/withdraw")
    public WalletResponse withdraw(@AuthenticationPrincipal User user, @Valid @RequestBody AmountRequest req) {
        return walletService.withdraw(user, req.amount());
    }
}
