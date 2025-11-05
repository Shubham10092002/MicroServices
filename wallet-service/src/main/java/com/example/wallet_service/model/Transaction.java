package com.example.wallet_service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Entity
@Table(name = "transactions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "transactions", "user"})
    private Wallet wallet;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(length = 255)
    private String description;

    @Column(name = "timestamp", columnDefinition = "TIMESTAMP(6)")
    private LocalDateTime timestamp;


    public enum Type {
        CREDIT, DEBIT, TRANSFER
    }

    public Transaction() {}

    public Transaction(Wallet wallet, BigDecimal amount, Type type, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Invalid amount: must be greater than 0");
        if (wallet == null)
            throw new IllegalArgumentException("Wallet cannot be null");
        if (type == null)
            throw new IllegalArgumentException("Transaction type must be specified");

        this.wallet = wallet;
        this.amount = amount;
        this.type = type;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now(ZoneOffset.UTC);
    }



    public Long getId() { return id; }
    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", amount=" + amount +
                ", type=" + type +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
