package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.dto.response.inventory.InventoryResponse;
import com.ttthinh.shoe_shop_basic.entity.inventory.Inventory;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.InventoryMapper;
import com.ttthinh.shoe_shop_basic.repository.jpa.InventoryRepository;
import com.ttthinh.shoe_shop_basic.service.InventoryService;
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
    public InventoryResponse getInventory(String variantSizeId) {
        var result =  inventoryRepository.getInventoryByVariantSize_Id(variantSizeId);
        return inventoryMapper.inventoryToInventory(result);
    }
    @Transactional
    @Override
    public InventoryResponse increaseStock(String variantSizeId, int amount) {
        if (amount <= 0) throw new AppException(ErrorCode.QUANTITY_NOT_VALID);

        Inventory inventory = inventoryRepository
                .findByVariantSizeId(variantSizeId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        inventory.setQuantity(inventory.getQuantity() + amount);
        return inventoryMapper.inventoryToInventory(inventory);
    }
    @Transactional
    @Override
    public InventoryResponse decreaseStock(String variantSizeId, int amount) {
        try{
            if (amount <= 0) throw new AppException(ErrorCode.QUANTITY_NOT_VALID);

            Inventory inventory = inventoryRepository
                    .findByVariantSizeId(variantSizeId)
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
    public InventoryResponse setStock(String variantSizeId, int quantity) {
        if (quantity < 0) {
            throw new AppException(ErrorCode.QUANTITY_NOT_VALID);
        }

        Inventory inventory = inventoryRepository
                .findByVariantSizeId(variantSizeId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        inventory.setQuantity(quantity);
        return inventoryMapper.inventoryToInventory(inventory);
    }



}
