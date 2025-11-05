// src/main/java/com/example/digitalWalletDemo/util/TransactionUtils.java

package com.example.wallet_service.util;

import com.example.wallet_service.data.WalletTransaction;
import com.example.wallet_service.data.WalletTransaction.Type;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionUtils {

    /**
     * Calculates the total amount of transactions of a specific type.
     */
    public static BigDecimal calculateTotal(List<WalletTransaction> transactions, Type type) {
        return transactions.stream()
                .filter(t -> t.type() == type) // Filter by type
                .map(WalletTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add); // Aggregate using reduce
    }

    /**
     * Filters transactions based on a minimum amount.
     */
    public static List<WalletTransaction> filterByMinAmount(List<WalletTransaction> transactions, BigDecimal minAmount) {
        return transactions.stream()
                .filter(t -> t.amount().compareTo(minAmount) >= 0)
                .collect(Collectors.toList());
    }
}