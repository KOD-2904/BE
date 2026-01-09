package com.ttthinh.shoe_shop_basic.repository.shop;

import com.ttthinh.shoe_shop_basic.entity.Brand;
import com.ttthinh.shoe_shop_basic.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, String> {
    boolean existsByName(String name);
}
