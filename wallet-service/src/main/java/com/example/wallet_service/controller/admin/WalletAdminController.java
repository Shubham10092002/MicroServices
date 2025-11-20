package com.example.wallet_service.controller.admin;

import com.example.wallet_service.dto.walletDto.CreateWalletDTO;
import com.example.wallet_service.model.wallet.Wallet;
import com.example.wallet_service.security.UserPrincipal;
import com.example.wallet_service.service.walletService.WalletService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wallets")
public class WalletAdminController {

    private static final Logger logger = LoggerFactory.getLogger(WalletAdminController.class);

    private final WalletService walletService;

    public WalletAdminController(WalletService walletService) {
        this.walletService = walletService;
    }

    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }


    @PatchMapping("/admin/{id}/blacklist")
    public ResponseEntity<?> toggleBlacklist(@PathVariable Long id, @RequestParam boolean status) {

        UserPrincipal principal = getCurrentUser();

        if (!"ADMIN".equalsIgnoreCase(principal.getRole())) {
            return ResponseEntity.status(403).body(Map.of(
                    "errorCode", "ACCESS_DENIED",
                    "reason", "Only ADMIN can modify wallet blacklist status"
            ));
        }

        var wallet = walletService.toggleBlacklistStatus(id, status);
        String msg = status ? "Wallet blacklisted successfully" : "Wallet removed from blacklist";

        return ResponseEntity.ok(Map.of(
                "message", msg,
                "walletId", wallet.getId(),
                "blacklisted", wallet.isBlacklisted()
        ));
    }


    // 2️⃣ Admin creates wallet for any user
    @PostMapping("/admin/create")
    public ResponseEntity<?> createWalletForUserAsAdmin(
            @RequestParam Long userId,
            @Valid @RequestBody CreateWalletDTO walletDTO
    ) {
        UserPrincipal principal = getCurrentUser();

        if (!"ADMIN".equalsIgnoreCase(principal.getRole())) {
            return ResponseEntity.status(403).body(Map.of(
                    "errorCode", "ACCESS_DENIED",
                    "reason", "Only ADMIN can create wallets for other users"
            ));
        }

        Wallet wallet = walletService.createWalletForUser(userId, walletDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Wallet created successfully for user " + userId,
                "walletId", wallet.getId(),
                "walletName", wallet.getWalletName(),
                "balance", wallet.getBalance()
        ));


    }


}

