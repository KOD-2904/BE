package com.ttthinh.shoe_shop_basic.repository;

import com.ttthinh.shoe_shop_basic.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    Optional<Role> findByCode(String code);

    boolean existsByCode(String role);

}
