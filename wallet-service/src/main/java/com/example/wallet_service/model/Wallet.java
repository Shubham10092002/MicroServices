package com.example.wallet_service.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String walletName;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    // 🔹 Only store userId, no relation to User entity
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 🔹 Optimistic locking version field
    @Version
    @Column(nullable = true)
    private Long version = 0L;

    // 🔹 Constructors
    public Wallet() {}

    public Wallet(String walletName, BigDecimal balance, Long userId) {
        this.walletName = walletName;
        this.balance = balance;
        this.userId = userId;
    }

    // ✅ Add this for test convenience
    public Wallet(Long id, String walletName, BigDecimal balance, Long userId) {
        this.id = id;
        this.walletName = walletName;
        this.balance = balance;
        this.userId = userId;
    }

    // 🔹 Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWalletName() { return walletName; }
    public void setWalletName(String walletName) { this.walletName = walletName; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
