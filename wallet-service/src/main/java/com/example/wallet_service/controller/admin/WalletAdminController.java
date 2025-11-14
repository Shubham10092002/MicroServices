package com.example.wallet_service.controller.admin;

import com.example.wallet_service.service.walletService.WalletService;
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


}

