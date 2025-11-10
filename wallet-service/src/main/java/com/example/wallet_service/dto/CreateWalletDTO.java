package com.example.wallet_service.dto;

import com.example.wallet_service.validation.ValidWalletBalance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CreateWalletDTO {

    @NotBlank(message = "walletName is required")
    private String walletName;

    @NotNull(message = "initialBalance is required")
    @ValidWalletBalance
    private BigDecimal initialBalance;

    public CreateWalletDTO() {}

    // ✅ Convenience constructor for tests
    public CreateWalletDTO(String walletName, BigDecimal initialBalance) {
        this.walletName = walletName;
        this.initialBalance = initialBalance;
    }

    // getters / setters
    public String getWalletName() { return walletName; }
    public void setWalletName(String walletName) { this.walletName = walletName; }
    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }
}
