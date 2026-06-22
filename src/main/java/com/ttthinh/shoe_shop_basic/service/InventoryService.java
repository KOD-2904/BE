package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.dto.response.inventory.InventoryResponse;
import com.ttthinh.shoe_shop_basic.entity.inventory.Inventory;
import org.springframework.transaction.annotation.Transactional;

public interface InventoryService {
    InventoryResponse increaseStock(String variantSizeId, int amount);
    InventoryResponse getInventory(String variantSizeId);

    @Transactional
    InventoryResponse decreaseStock(String variantSizeId, int amount);

    @Transactional
    InventoryResponse setStock(String variantSizeId, int quantity);
}
