package com.ttthinh.shoe_shop_basic.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {UserNameValidator.class}) // Liên kết với validator
public @interface UserNameConstraint {
    String message() default "Tên đăng nhập không hợp lệ";

    // Validation groups
    Class<?>[] groups() default {};

    // Payload
    Class<? extends Payload>[] payload() default {};

    // Tùy chọn cấu hình
    int min() default 4;
    int max() default 20;
    boolean allowSpecialChars() default false;
}
