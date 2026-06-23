package com.ttthinh.shoe_shop_basic.repository.jpa;

import com.ttthinh.shoe_shop_basic.entity.inventory.Inventory;
import com.ttthinh.shoe_shop_basic.entity.catalog.VariantSize;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {
    Inventory getInventoryByVariantSize_Id(String variantSizeId);

    Optional<Inventory> findByVariantSizeId(String variantSizeId);

    Optional<Inventory> findByVariantSize(VariantSize variantSize);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.variantSize.id = :variantSizeId")
    Optional<Inventory> findLockedByVariantSizeId(@Param("variantSizeId") String variantSizeId);
}
