package com.example.wallet_service.controller;

import com.example.wallet_service.dto.TransactionDTO;
import com.example.wallet_service.model.Wallet;
import com.example.wallet_service.security.UserPrincipal;
import com.example.wallet_service.service.TransactionService;
import com.example.wallet_service.repository.WalletRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final WalletRepository walletRepository;

    public TransactionController(TransactionService transactionService, WalletRepository walletRepository) {
        this.transactionService = transactionService;
        this.walletRepository = walletRepository;
    }

    // 🔹 Helper to get current authenticated user
    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // 🔹 Helper for 403 Forbidden response
    private ResponseEntity<?> forbidden(String message) {
        return ResponseEntity.status(403).body(Map.of(
                "errorCode", "ACCESS_DENIED",
                "reason", message
        ));
    }

    // 🔹 Helper for 404 Not Found response
    private ResponseEntity<?> notFound(String message) {
        return ResponseEntity.status(404).body(Map.of(
                "errorCode", "NOT_FOUND",
                "reason", message
        ));
    }

    // ======================== GET ALL TRANSACTIONS (ADMIN ONLY) ========================
    @GetMapping
    public ResponseEntity<?> getAllTransactions() {
        UserPrincipal principal = getCurrentUser();
        if (!"ADMIN".equalsIgnoreCase(principal.getRole())) {
            return forbidden("Only ADMIN can view all transactions");
        }
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    // ======================== GET TRANSACTIONS BY WALLET ID ========================
    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<?> getTransactionsByWallet(@PathVariable Long walletId) {
        UserPrincipal principal = getCurrentUser();

        Wallet wallet = walletRepository.findById(walletId).orElse(null);
        if (wallet == null) {
            return notFound("Wallet not found with ID " + walletId);
        }

        // ✅ Ownership check
        if (!"ADMIN".equalsIgnoreCase(principal.getRole()) &&
                !wallet.getUserId().equals(principal.getUserId())) {
            return forbidden("You can only view transactions of your own wallets");
        }

        return ResponseEntity.ok(transactionService.getTransactionsByWallet(walletId));
    }

    // ======================== GET TRANSACTION HISTORY (ADMIN ONLY) ========================
    @GetMapping("/history")
    public ResponseEntity<?> getTransactionHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long walletId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UserPrincipal principal = getCurrentUser();

        if (!"ADMIN".equalsIgnoreCase(principal.getRole())) {
            return forbidden("Only ADMIN can access transaction history");
        }

        try {
            return ResponseEntity.ok(transactionService.getTransactionHistory(
                    userId, walletId, type, start, end, page, size
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "INVALID_REQUEST",
                    "reason", e.getMessage()
            ));
        }
    }

    // ======================== GET USER TRANSACTIONS (USER or ADMIN) ========================
    @GetMapping("/user/{userId}/transactions")
    public ResponseEntity<?> getUserTransactions(
            @PathVariable Long userId,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) String type
    ) {
        UserPrincipal principal = getCurrentUser();

        // ✅ Only the same user or ADMIN can access
        if (!"ADMIN".equalsIgnoreCase(principal.getRole()) &&
                !userId.equals(principal.getUserId())) {
            return forbidden("You can only view your own transactions");
        }

        Object response = transactionService.getUserTransactions(userId, start, end, type);
        if (response instanceof String) {
            return ResponseEntity.badRequest().body(Map.of(
                    "errorCode", "INVALID_INPUT",
                    "reason", response
            ));
        }

        return ResponseEntity.ok(response);
    }
}
