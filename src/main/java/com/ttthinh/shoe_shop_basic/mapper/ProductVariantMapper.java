package com.ttthinh.shoe_shop_basic.mapper;

import com.ttthinh.shoe_shop_basic.dto.response.shop.ProductResponse;
import com.ttthinh.shoe_shop_basic.dto.response.shop.ProductVariantResponse;
import com.ttthinh.shoe_shop_basic.entity.product.Product;
import com.ttthinh.shoe_shop_basic.entity.product.ProductVariant;
import com.ttthinh.shoe_shop_basic.enums.ProductStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {
    @Mapping(source = "product.id", target = "productId")
    public ProductVariantResponse toProductVariantResponse(ProductVariant productVariant);
    @Mapping(source = "product.id", target = "productId")
    public List<ProductVariantResponse> toProductVariantsResponse(List<ProductVariant> variantList);

    //@Named("mapProductStatusToString")
    //default String mapProductStatusToString(ProductStatus status) {
        //return status != null ? status.name() : null;
    //}
}
