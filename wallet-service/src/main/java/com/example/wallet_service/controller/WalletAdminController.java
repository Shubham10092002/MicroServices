package com.example.wallet_service.controller;

import com.example.wallet_service.model.Wallet;
import com.example.wallet_service.exception.WalletIdNotFoundException;
import com.example.wallet_service.repository.WalletRepository;
import com.example.wallet_service.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/wallets")
public class WalletAdminController {

    private static final Logger logger = LoggerFactory.getLogger(WalletAdminController.class);

    private final WalletService walletService;

    public WalletAdminController(WalletService walletService) {
        this.walletService = walletService;
    }


    @PatchMapping("/{id}/blacklist")
    public ResponseEntity<?> toggleBlacklist(@PathVariable Long id, @RequestParam boolean status) {
        var wallet = walletService.toggleBlacklistStatus(id, status);
        String msg = status ? "Wallet blacklisted successfully" : "Wallet removed from blacklist";
        return ResponseEntity.ok(Map.of("message", msg, "walletId", wallet.getId(), "blacklisted", wallet.isBlacklisted()));
    }

//    @PatchMapping("/{walletId}/blacklist")
//    public ResponseEntity<?> blacklistWallet(@PathVariable Long walletId) {
//        Wallet wallet = walletRepository.findById(walletId)
//                .orElseThrow(() -> new WalletIdNotFoundException("Wallet not found"));
//
//        wallet.setBlacklisted(true);
//        walletRepository.save(wallet);
//
//        return ResponseEntity.ok(Map.of(
//                "message", "Wallet blacklisted successfully",
//                "walletId", wallet.getId()
//        ));
//    }

//    @PatchMapping("/{id}/blacklist")
//    public ResponseEntity<?> toggleBlacklist(@PathVariable Long id, @RequestParam boolean status) {
//        var wallet = WalletService.toggleBlacklistStatus(id, status);
//        String msg = status ? "User blacklisted successfully" : "User removed from blacklist";
//        return ResponseEntity.ok(Map.of("message", msg, "userId", user.getId(), "blacklisted", user.isBlacklisted()));
//    }

//    @PatchMapping("/{walletId}/unblacklist")
//    public ResponseEntity<?> unblacklistWallet(@PathVariable Long walletId) {
//        Wallet wallet = walletRepository.findById(walletId)
//                .orElseThrow(() -> new WalletIdNotFoundException("Wallet not found"));
//
//        wallet.setBlacklisted(false);
//        walletRepository.save(wallet);
//
//        return ResponseEntity.ok(Map.of(
//                "message", "Wallet unblacklisted successfully",
//                "walletId", wallet.getId()
//        ));
//    }
}

