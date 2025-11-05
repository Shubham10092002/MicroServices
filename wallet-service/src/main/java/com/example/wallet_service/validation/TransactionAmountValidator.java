package com.example.wallet_service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class TransactionAmountValidator implements ConstraintValidator<ValidTransactionAmount, BigDecimal> {

    private final BigDecimal MIN = new BigDecimal("10");
    private final BigDecimal MAX = new BigDecimal("50000");

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return (value.compareTo(MIN) >= 0) && (value.compareTo(MAX) <= 0);
    }
}
