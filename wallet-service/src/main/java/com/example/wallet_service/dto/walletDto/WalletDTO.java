package com.example.wallet_service.dto.walletDto;

import java.math.BigDecimal;

public class WalletDTO {
    private Long id;
    private String walletName;
    private BigDecimal balance;
    private Long userId;

    public WalletDTO() {}

    public WalletDTO(Long id, String walletName, BigDecimal balance, Long userId) {
        this.id = id;
        this.walletName = walletName;
        this.balance = balance;
        this.userId = userId;
    }

    // getters & setters

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
