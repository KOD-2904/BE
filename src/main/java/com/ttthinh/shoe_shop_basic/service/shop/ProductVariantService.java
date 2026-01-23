package com.ttthinh.shoe_shop_basic.service.shop;

import com.ttthinh.shoe_shop_basic.dto.request.shop.ProductVariantRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shop.ProductVariantResponse;
import com.ttthinh.shoe_shop_basic.entity.product.ProductVariant;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductVariantService {
    public ProductVariantResponse addVariantImages(List<MultipartFile> images, String variantId, Integer primaryIndex) ;

    public ProductVariantResponse getProductVariant(String id);
    public List<ProductVariantResponse> getAllProductVariant();
    public List<ProductVariantResponse> getProductVariantByProduct(String product);
    public ProductVariantResponse addProductVariant(ProductVariantRequest productVariantRequest, Integer initQuantity);
    public List<ProductVariantResponse> addProductVariants(List<ProductVariantRequest> productVariantRequest, Integer initQuantity);
    public ProductVariantResponse updateProductVariant(String id, ProductVariantRequest productVariantRequest);
}
