package com.ttthinh.shoe_shop_basic.service.shop;

import com.ttthinh.shoe_shop_basic.dto.response.shop.InventoryResponse;
import com.ttthinh.shoe_shop_basic.entity.product.Inventory;
import org.springframework.transaction.annotation.Transactional;

public interface InventoryService {
    InventoryResponse increaseStock(String variantId, int amount);
    InventoryResponse getInventory(String variantId);

    @Transactional
    InventoryResponse decreaseStock(String variantId, int amount);

    @Transactional
    InventoryResponse setStock(String variantId, int quantity);
}
