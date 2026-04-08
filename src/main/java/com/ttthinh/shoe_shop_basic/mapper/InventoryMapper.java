package com.ttthinh.shoe_shop_basic.mapper;

import com.ttthinh.shoe_shop_basic.dto.response.shop.InventoryResponse;
import com.ttthinh.shoe_shop_basic.entity.product.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {
    @Mapping(source = "variant.id", target = "variantId")
    public InventoryResponse inventoryToInventory(Inventory inventory);
}
