package com.example.user_service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

public class UserAgeValidator implements ConstraintValidator<ValidUserAge, LocalDate> {

    @Override
    public boolean isValid(LocalDate dob, ConstraintValidatorContext context) {
        if (dob == null) return true; // let @NotNull handle it if required
        return Period.between(dob, LocalDate.now()).getYears() >= 18;
    }
}
