package com.ttthinh.shoe_shop_basic.mapper;

import com.ttthinh.shoe_shop_basic.dto.response.cart.CartItemResponse;
import com.ttthinh.shoe_shop_basic.entity.catalog.Brand;
import com.ttthinh.shoe_shop_basic.entity.catalog.Category;
import com.ttthinh.shoe_shop_basic.entity.cart.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CartItemMapper {

    @Mapping(target = "cartItemId", source = "id")
    @Mapping(target = "variantSizeId", source = "variantSize.id")

    @Mapping(target = "productName", source = "variantSize.variant.product.name")
    @Mapping(target = "brand", source = "variantSize.variant.product.brand", qualifiedByName = "mapBrandName")
    @Mapping(target = "category", source = "variantSize.variant.product.category", qualifiedByName = "mapCategoryName")

    @Mapping(target = "size", source = "variantSize.size")
    @Mapping(target = "color", source = "variantSize.variant.color")

    @Mapping(target = "price", source = "variantSize.price")
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
        if (item == null || item.getVariantSize() == null) return BigDecimal.ZERO;
        return item.getVariantSize().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}

