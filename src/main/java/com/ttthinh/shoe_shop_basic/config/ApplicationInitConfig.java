package com.ttthinh.shoe_shop_basic.config;

import com.ttthinh.shoe_shop_basic.entity.auth.Role;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.enums.AuthProvider;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.repository.jpa.RoleRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.UserAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;

@Configuration
@Slf4j
public class ApplicationInitConfig {
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.admin-email:admin@shoe-shop.local}")
    private String adminEmail;

    @Value("${app.init.admin-password:ChangeMe123!}")
    private String adminPassword;

    @Value("${app.init.demo-email:demo@shoe-shop.local}")
    private String demoEmail;

    @Value("${app.init.demo-password:ChangeMe123!}")
    private String demoPassword;

    public ApplicationInitConfig(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.init", name = "enabled", havingValue = "true", matchIfMissing = true)
    ApplicationRunner runner(UserAccountRepository userAccountRepository, RoleRepository roleRepository) {
        return args -> {
            log.info("===== START INITIALIZING DATABASE =====");

            Role roleUser = getOrCreateRole(roleRepository, "ROLE_USER", "User", "Default user role");
            Role roleAdmin = getOrCreateRole(roleRepository, "ROLE_ADMIN", "Admin", "Administrator");
            getOrCreateRole(roleRepository, "ROLE_STAFF", "Staff", "Staff");

            if (!userAccountRepository.existsByEmail(adminEmail)) {
                UserAccount admin = new UserAccount();
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setPhone("0123456789");
                admin.setProvider(AuthProvider.LOCAL);
                admin.setStatus(UserStatus.ACTIVE);
                admin.setEmailVerified(true);

                HashSet<Role> roles = new HashSet<>();
                roles.add(roleAdmin);
                roles.add(roleUser);
                admin.setRoles(roles);

                userAccountRepository.save(admin);
                log.info("Created initial admin user with email: {}", adminEmail);
            } else {
                log.info("Initial admin user already exists");
            }

            if (!userAccountRepository.existsByEmail(demoEmail)) {
                UserAccount user = new UserAccount();
                user.setEmail(demoEmail);
                user.setPassword(passwordEncoder.encode(demoPassword));
                user.setPhone("0987654321");
                user.setProvider(AuthProvider.LOCAL);
                user.setStatus(UserStatus.ACTIVE);
                user.setEmailVerified(true);

                HashSet<Role> roles = new HashSet<>();
                roles.add(roleUser);
                user.setRoles(roles);

                userAccountRepository.save(user);
                log.info("Created initial demo user with email: {}", demoEmail);
            } else {
                log.info("Initial demo user already exists");
            }

            log.info("===== DATABASE INITIALIZATION COMPLETED =====");
        };
    }

    private Role getOrCreateRole(RoleRepository roleRepository, String code, String name, String description) {
        return roleRepository.findByCode(code)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setCode(code);
                    role.setName(name);
                    role.setDescription(description);
                    roleRepository.save(role);
                    log.info("Created {}", code);
                    return role;
                });
    }
}
