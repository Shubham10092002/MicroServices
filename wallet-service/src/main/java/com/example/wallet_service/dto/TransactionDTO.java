package com.example.wallet_service.dto;

import com.example.wallet_service.model.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public class TransactionDTO {


    private Long id;
    private Long walletId;
    private BigDecimal amount;
    private String type;
    private String description;
    private LocalDateTime timestamp;


    public TransactionDTO() {}

    public TransactionDTO(Transaction transaction) {
        this.id = transaction.getId();
        this.walletId = transaction.getWallet() != null ? transaction.getWallet().getId() : null;
        this.amount = transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO;
        this.type = transaction.getType() != null ? transaction.getType().name() : "UNKNOWN";
        this.description = transaction.getDescription() != null ? transaction.getDescription() : "";
        this.timestamp = transaction.getTimestamp() != null ? transaction.getTimestamp() : LocalDateTime.now();
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setType(String type) {
        this.type = type;
    }

    // Getters
    public Long getId() { return id; }
    public Long getWalletId() { return walletId; }
    public BigDecimal getAmount() { return amount; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
