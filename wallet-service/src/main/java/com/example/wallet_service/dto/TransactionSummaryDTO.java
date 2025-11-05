package com.example.wallet_service.dto;

import java.math.BigDecimal;
import com.example.wallet_service.model.Transaction;

public record TransactionSummaryDTO(
        Transaction.Type type,
        BigDecimal totalAmount
) {}
