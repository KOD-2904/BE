package com.ttthinh.shoe_shop_basic.dto.response;

import com.ttthinh.shoe_shop_basic.entity.Role;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String status;
    private Set<String> roles;
}
