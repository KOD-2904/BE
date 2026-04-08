package com.ttthinh.shoe_shop_basic.repository.shop;

import com.ttthinh.shoe_shop_basic.entity.product.Inventory;
import com.ttthinh.shoe_shop_basic.entity.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {
    Inventory getInventoryByVariant_Id(String variantId);

    Optional<Inventory> findByVariantId(String variantId);

    Optional<Inventory> findByVariant(ProductVariant variant);
}