package com.ttthinh.shoe_shop_basic.config;

import com.ttthinh.shoe_shop_basic.entity.auth.Role;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.auth.RoleRepository;
import com.ttthinh.shoe_shop_basic.repository.auth.UserAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;

@Configuration
@Slf4j
public class ApplicationInitConfig {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner runner(UserAccountRepository userAccountRepository, RoleRepository roleRepository) {
        return args -> {
            log.info("===== START INITIALIZING DATABASE =====");

            // 1. INIT ROLES
            Role roleUser = null;
            Role roleAdmin = null;

            if (!roleRepository.existsByCode("ROLE_USER")) {
                roleUser = new Role();
                roleUser.setCode("ROLE_USER");
                roleUser.setName("User");
                roleUser.setDescription("Default user role");
                roleRepository.save(roleUser);
                log.info("Created ROLE_USER");
            } else {
                roleUser = roleRepository.findByCode("ROLE_USER")
                        .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXIST));
            }

            if (!roleRepository.existsByCode("ROLE_ADMIN")) {
                roleAdmin = new Role();
                roleAdmin.setCode("ROLE_ADMIN");
                roleAdmin.setName("Admin");
                roleAdmin.setDescription("Administrator");
                roleRepository.save(roleAdmin);
                log.info("Created ROLE_ADMIN");
            } else {
                roleAdmin = roleRepository.findByCode("ROLE_ADMIN")
                        .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXIST));
            }

            // 2. INIT ADMIN USER
            if (!userAccountRepository.existsByUsername("admin")) {
                UserAccount admin = new UserAccount();
                admin.setUsername("admin");
                admin.setEmail("admin@shoe-shop.com"); // Email bắt buộc
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setFirstName("System");
                admin.setLastName("Administrator");
                admin.setPhone("0123456789");
                admin.setStatus(UserStatus.ACTIVE);
                admin.setEmailVerified(true);

                // Set roles
                HashSet<Role> roles = new HashSet<>();
                roles.add(roleAdmin);
                roles.add(roleUser); // Admin có cả 2 role (tùy chọn)
                admin.setRoles(roles);

                userAccountRepository.save(admin);
                log.info("Created admin user with username: admin, password: admin");
            } else {
                log.info("Admin user already exists");
            }

            // 3. INIT TEST USER (Optional)
            if (!userAccountRepository.existsByUsername("user")) {
                UserAccount user = new UserAccount();
                user.setUsername("user");
                user.setEmail("user@shoe-shop.com");
                user.setPassword(passwordEncoder.encode("user"));
                user.setFirstName("Test");
                user.setLastName("User");
                user.setPhone("0987654321");
                user.setStatus(UserStatus.ACTIVE);
                user.setEmailVerified(true);

                HashSet<Role> roles = new HashSet<>();
                roles.add(roleUser);
                user.setRoles(roles);

                userAccountRepository.save(user);
                log.info("Created test user with username: user, password: user");
            } else {
                log.info("Test user already exists");
            }

            log.info("===== DATABASE INITIALIZATION COMPLETED =====");
        };
    }
}