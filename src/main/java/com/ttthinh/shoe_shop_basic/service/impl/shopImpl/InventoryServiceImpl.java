package com.ttthinh.shoe_shop_basic.service.impl.shopImpl;

import com.ttthinh.shoe_shop_basic.dto.response.shop.InventoryResponse;
import com.ttthinh.shoe_shop_basic.entity.product.Inventory;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.InventoryMapper;
import com.ttthinh.shoe_shop_basic.repository.shop.InventoryRepository;
import com.ttthinh.shoe_shop_basic.service.shop.InventoryService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
     private final InventoryRepository inventoryRepository;
     private final InventoryMapper inventoryMapper;

    @Override
    public InventoryResponse getInventory(String variantId) {
        var result =  inventoryRepository.getInventoryByVariant_Id(variantId);
        return inventoryMapper.inventoryToInventory(result);
    }
    @Transactional
    @Override
    public InventoryResponse increaseStock(String variantId, int amount) {
        if (amount <= 0) throw new AppException(ErrorCode.QUANTITY_NOT_VALID);

        Inventory inventory = inventoryRepository
                .findByVariantId(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        inventory.setQuantity(inventory.getQuantity() + amount);
        return inventoryMapper.inventoryToInventory(inventory);
    }
    @Transactional
    @Override
    public InventoryResponse decreaseStock(String variantId, int amount) {
        try{
            if (amount <= 0) throw new AppException(ErrorCode.QUANTITY_NOT_VALID);

            Inventory inventory = inventoryRepository
                    .findByVariantId(variantId)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

            if (inventory.getQuantity() < amount) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }

            inventory.setQuantity(inventory.getQuantity() - amount);
            return inventoryMapper.inventoryToInventory(inventory);
        }
        catch (OptimisticLockException e) {
            throw new AppException(ErrorCode.OUT_OF_STOCK);
        }
    }
    @Transactional
    @Override
    public InventoryResponse setStock(String variantId, int quantity) {
        if (quantity < 0) {
            throw new AppException(ErrorCode.QUANTITY_NOT_VALID);
        }

        Inventory inventory = inventoryRepository
                .findByVariantId(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        inventory.setQuantity(quantity);
        return inventoryMapper.inventoryToInventory(inventory);
    }



}
