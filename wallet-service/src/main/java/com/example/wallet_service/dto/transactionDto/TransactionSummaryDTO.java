package com.example.wallet_service.dto.transactionDto;

import java.math.BigDecimal;
import com.example.wallet_service.model.transaction.Transaction;

public record TransactionSummaryDTO(
        Transaction.Type type,
        BigDecimal totalAmount
) {}
