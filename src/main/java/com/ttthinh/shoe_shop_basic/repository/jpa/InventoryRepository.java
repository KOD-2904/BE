package com.ttthinh.shoe_shop_basic.repository.jpa;

import com.ttthinh.shoe_shop_basic.entity.inventory.Inventory;
import com.ttthinh.shoe_shop_basic.entity.catalog.VariantSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {
    Inventory getInventoryByVariantSize_Id(String variantSizeId);

    Optional<Inventory> findByVariantSizeId(String variantSizeId);

    Optional<Inventory> findByVariantSize(VariantSize variantSize);
}
