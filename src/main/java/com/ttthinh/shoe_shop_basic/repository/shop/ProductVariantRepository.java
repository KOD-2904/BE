package com.ttthinh.shoe_shop_basic.repository.shop;

import com.ttthinh.shoe_shop_basic.entity.Category;
import com.ttthinh.shoe_shop_basic.entity.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
    List<ProductVariant> findByProductId(String productId);
}
