package com.example.wallet_service.data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Using a Java 21 Record for an immutable data carrier
public record WalletTransaction(
        Long id,
        Long walletId,
        BigDecimal amount,
        Type type,
        String description,
        LocalDateTime timestamp
) {
    // Enum for Transaction Type (e.g., CREDIT, DEBIT)
    public enum Type {
        CREDIT, DEBIT, TRANSFER
    }

    // Canonical constructor for validation logic
    public WalletTransaction {
        if (walletId == null || walletId <= 0) {
            throw new IllegalArgumentException("Wallet ID must be valid.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Transaction type must be specified.");
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now(); // Default if not provided
        }
    }
}