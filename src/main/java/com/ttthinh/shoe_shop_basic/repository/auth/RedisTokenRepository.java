package com.ttthinh.shoe_shop_basic.repository.auth;

import com.ttthinh.shoe_shop_basic.entity.auth.RedisToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RedisTokenRepository extends CrudRepository<RedisToken, String> {
    // Tìm tất cả refresh tokens của user
    List<RedisToken> findByUserId(String userId);

    // Tìm refresh token theo userId và deviceId
    Optional<RedisToken> findByUserIdAndDeviceId(String userId, String deviceId);

    // Tìm active refresh tokens
    List<RedisToken> findByUserIdAndActiveTrue(String userId);

    // Xóa tất cả tokens của user
    void deleteAllByUserId(String userId);

    // Kiểm tra token có tồn tại và active không
    boolean existsByIdAndActiveTrue(String id);

    Iterable<? extends RedisToken> findByTokenType(String tokenType);
}
