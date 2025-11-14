package com.example.wallet_service.validation.transactionValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = TransactionAmountValidator.class)
public @interface ValidTransactionAmount {
    String message() default "Transaction amount must be between 10 and 50000";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
