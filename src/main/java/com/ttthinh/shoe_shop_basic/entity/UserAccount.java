package com.ttthinh.shoe_shop_basic.entity;

import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.validation.UserNameConstraint;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "user_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_user_phone", columnNames = "phone")
        }
)

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserAccount extends BaseEntity {

    @Column(name = "username", nullable = false, length = 100)
    @UserNameConstraint
    private String username;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "firstname", length = 255)
    private String firstName;

    @Column(name = "lastname", length = 255)
    private String lastName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.INACTIVE;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
            uniqueConstraints = {
                    @UniqueConstraint(name = "pk_user_role", columnNames = {"user_id", "role_id"})
            }
    )
    private Set<Role> roles = new HashSet<>();

}
