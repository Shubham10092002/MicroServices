package com.example.user_service.dto;

import java.math.BigDecimal;

public class Wallet {

    private Long id;
    private String walletName;
    private BigDecimal balance;
    private Long userId;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWalletName() { return walletName; }
    public void setWalletName(String walletName) { this.walletName = walletName; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
