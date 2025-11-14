package com.example.wallet_service.dto.transactionDto;

import com.example.wallet_service.validation.transactionValidator.ValidTransactionAmount;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class TransactionRequestDTO {

    @NotNull(message = "walletId is required")
    private Long walletId;

    @NotNull(message = "amount is required")
    @ValidTransactionAmount
    private BigDecimal amount;

    @NotNull(message = "type is required")
    @Size(min = 3, max = 10)
    private String type;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    // getters / setters
    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
