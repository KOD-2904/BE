package com.ttthinh.shoe_shop_basic.repository.auth;

import com.ttthinh.shoe_shop_basic.entity.verify.EmailVerifyToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerifyRepository extends JpaRepository<EmailVerifyToken, String> {
    Optional<EmailVerifyToken> findByToken(String token);
}
