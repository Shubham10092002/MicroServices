package com.example.wallet_service.dto.walletDto;

import com.example.wallet_service.validation.walletBalancevalidator.ValidWalletBalance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class WalletRequestDTO {

    @NotBlank(message = "Wallet name is required")
    @Size(max = 100)
    private String walletName;

    @NotNull(message = "Balance is required")
    @ValidWalletBalance
    private BigDecimal balance;

    @NotNull(message = "userId is required")
    private Long userId;

    // getters / setters
    public String getWalletName() { return walletName; }
    public void setWalletName(String walletName) { this.walletName = walletName; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
