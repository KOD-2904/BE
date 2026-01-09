package com.ttthinh.shoe_shop_basic.dto.request.auth;

import com.ttthinh.shoe_shop_basic.validation.UserNameConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Validated
public class RegisterRequest {
    @NotBlank
    @UserNameConstraint(message = "USERNAME_NOT_VALID", min = 5)
    String username;
    @NotNull(message = "Mật khẩu không trống")
    String password;
    String email;
    String firstName;
    String lastName;
    String phone;
}
