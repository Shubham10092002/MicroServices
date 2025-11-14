package com.example.wallet_service.validation.walletBalancevalidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = WalletBalanceValidator.class)
public @interface ValidWalletBalance {
    String message() default "Wallet balance must be >= 0 and <= 1,000,000";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
