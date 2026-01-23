package com.ttthinh.shoe_shop_basic.mapper;

import com.ttthinh.shoe_shop_basic.dto.response.shop.ProductResponse;
import com.ttthinh.shoe_shop_basic.entity.Brand;
import com.ttthinh.shoe_shop_basic.entity.Category;
import com.ttthinh.shoe_shop_basic.entity.product.Product;
import com.ttthinh.shoe_shop_basic.enums.ProductStatus;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "brandId", source = "brand", qualifiedByName = "mapBrandId")
    @Mapping(target = "categoryId", source = "category", qualifiedByName = "mapCategoryId")
    ProductResponse toProductResponse(Product product);

    @Mapping(target = "brandId", source = "brand", qualifiedByName = "mapBrandId")
    @Mapping(target = "categoryId", source = "category", qualifiedByName = "mapCategoryId")
    List<ProductResponse> toProductResponse(List<Product> productList);

    @Named("mapBrandId")
    default String mapBrandId(Brand brand) {
        return String.valueOf(brand.getId());
    }
    @Named("mapCategoryId")
    default String mapCategoryId(Category category) {
        return String.valueOf(category.getId());
    }
    @Named("mapProductStatusToString")
    default String mapProductStatusToString(ProductStatus status) {
        return status != null ? status.name() : null;
    }
}
