package com.ttthinh.shoe_shop_basic.mapper;

import com.ttthinh.shoe_shop_basic.dto.response.shop.CartItemResponse;
import com.ttthinh.shoe_shop_basic.entity.Brand;
import com.ttthinh.shoe_shop_basic.entity.Category;
import com.ttthinh.shoe_shop_basic.entity.cart.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CartItemMapper {

    @Mapping(target = "cartItemId", source = "id")
    @Mapping(target = "variantId", source = "variant.id")

    @Mapping(target = "productName", source = "variant.product.name")
    @Mapping(target = "brand", source = "variant.product.brand", qualifiedByName = "mapBrandName")
    @Mapping(target = "category", source = "variant.product.category", qualifiedByName = "mapCategoryName")

    @Mapping(target = "size", source = "variant.size")
    @Mapping(target = "color", source = "variant.color")

    @Mapping(target = "price", source = "variant.price")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "lineTotal", source = ".", qualifiedByName = "mapLineTotal")
    CartItemResponse toResponse(CartItem cartItem);

    List<CartItemResponse> toResponse(List<CartItem> items);

    /* ================== CUSTOM MAPPERS ================== */

    @Named("mapBrandName")
    default String mapBrandName(Brand brand) {
        return brand != null ? brand.getName() : null;
    }

    @Named("mapCategoryName")
    default String mapCategoryName(Category category) {
        return category != null ? category.getName() : null;
    }

    @Named("mapLineTotal")
    default BigDecimal mapLineTotal(CartItem item) {
        if (item == null || item.getVariant() == null) return BigDecimal.ZERO;
        return item.getVariant().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}

