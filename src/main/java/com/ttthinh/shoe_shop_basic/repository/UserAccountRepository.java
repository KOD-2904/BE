package com.ttthinh.shoe_shop_basic.repository;

import com.ttthinh.shoe_shop_basic.entity.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
    Optional<UserAccount> findByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findByUsername(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByUsername(String username);
}
