package com.ttthinh.shoe_shop_basic.validation;

import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

@Component
public class UserNameValidator implements ConstraintValidator<UserNameConstraint, String> {
    private int minLength;
    private int maxLength;
    private boolean allowSpecialChars;

    // Danh sách từ cấm
    private static final String[] FORBIDDEN_WORDS = {
            "admin", "administrator", "mod", "moderator",
            "root", "system", "test", "user",
            "fuck", "shit", "damn", "bitch"
    };

    public UserNameValidator() {
        System.out.println("✅ UsernameValidator constructor called!");
    }

    @Override
    public void initialize(UserNameConstraint constraint) {
        this.minLength = constraint.min();
        this.maxLength = constraint.max();
        this.allowSpecialChars = constraint.allowSpecialChars();
        System.out.println("✅ Validator initialized: min=" + minLength + ", max=" + maxLength);
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        System.out.println("🎯 Validating username: " + username);

        // Nếu null, để @NotBlank xử lý
        if (username == null) {
            return true;
        }

        // Reset context để tùy chỉnh message
        context.disableDefaultConstraintViolation();
        boolean isValid = true;

        // 1. Check độ dài
        if (username.length() < minLength) {
            addError(context, "Username phải có ít nhất " + minLength + " ký tự");
            isValid = false;
        }

        if (username.length() > maxLength) {
            addError(context, "Username không được quá " + maxLength + " ký tự");
            isValid = false;
        }

        // 2. Check bắt đầu bằng chữ cái
        if (!username.isEmpty() && Character.isDigit(username.charAt(0))) {
            addError(context, "Username không được bắt đầu bằng số");
            isValid = false;
        }

        // 3. Check ký tự hợp lệ
        String regex = allowSpecialChars ? "^[a-zA-Z0-9._-]+$" : "^[a-zA-Z0-9_]+$";
        if (!username.matches(regex)) {
            String allowed = allowSpecialChars ? "chữ cái, số, dấu gạch dưới (_), dấu chấm (.), dấu gạch ngang (-)"
                    : "chữ cái, số và dấu gạch dưới (_)";
            addError(context, "Username chỉ được chứa " + allowed);
            isValid = false;
        }

        // 4. Check khoảng trắng
        if (username.contains(" ")) {
            addError(context, "Username không được chứa khoảng trắng");
            isValid = false;
        }

        // 5. Check từ cấm
        String lowerUsername = username.toLowerCase();
        for (String forbidden : FORBIDDEN_WORDS) {
            if (lowerUsername.contains(forbidden)) {
                addError(context, "Username không được chứa từ '" + forbidden + "'");
                isValid = false;
                break;
            }
        }

        System.out.println("✅ Validation result: " + (isValid ? "PASS" : "FAIL"));
        return isValid;
    }

    private void addError(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}
