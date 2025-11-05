package com.example.wallet_service.controller;

import com.example.wallet_service.dto.TransactionDTO;
import com.example.wallet_service.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Get all transactions
    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    // Get transactions by wallet ID
    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByWallet(@PathVariable Long walletId) {
        return ResponseEntity.ok(transactionService.getTransactionsByWallet(walletId));
    }

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
        try {
            return ResponseEntity.ok(transactionService.getTransactionHistory(
                    userId, walletId, type, start, end, page, size
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Get transactions for a user between dates (optionally filtered by type)
    @GetMapping("/user/{userId}/transactions")
    public ResponseEntity<?> getUserTransactions(
            @PathVariable Long userId,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) String type
    ) {
        Object response = transactionService.getUserTransactions(userId, start, end, type);

        if (response instanceof String) {
            // return error message if invalid date/type
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }
}
