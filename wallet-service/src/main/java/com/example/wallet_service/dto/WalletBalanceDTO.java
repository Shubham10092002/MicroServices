package com.example.wallet_service.dto;

import java.math.BigDecimal;

public class WalletBalanceDTO {

    private Long walletId;
    private String walletName;
    private BigDecimal balance;

    public WalletBalanceDTO(Long walletId, String walletName, BigDecimal balance) {
        this.walletId = walletId;
        this.walletName = walletName;
        this.balance = balance;
    }

    public Long getWalletId() { return walletId; }
    public String getWalletName() { return walletName; }
    public BigDecimal getBalance() { return balance; }
}
