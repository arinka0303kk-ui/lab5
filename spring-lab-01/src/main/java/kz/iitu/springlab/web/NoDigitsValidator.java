package kz.iitu.springlab.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NoDigitsValidator implements ConstraintValidator<NoDigits, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return value.chars().noneMatch(Character::isDigit);
    }
}
