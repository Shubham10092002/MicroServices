package com.example.wallet_service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class WalletBalanceValidator implements ConstraintValidator<ValidWalletBalance, BigDecimal> {

    private final BigDecimal MIN = BigDecimal.ZERO;
    private final BigDecimal MAX = new BigDecimal("1000000");

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return value.compareTo(MIN) >= 0 && value.compareTo(MAX) <= 0;
    }
}
