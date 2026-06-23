package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.dto.request.catalog.ProductVariantRequest;
import com.ttthinh.shoe_shop_basic.dto.request.catalog.VariantSizeRequest;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductVariantResponse;
import com.ttthinh.shoe_shop_basic.entity.catalog.VariantSize;
import com.ttthinh.shoe_shop_basic.entity.inventory.Inventory;
import com.ttthinh.shoe_shop_basic.entity.catalog.Product;
import com.ttthinh.shoe_shop_basic.entity.catalog.ProductVariant;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.ProductVariantMapper;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductVariantRepository;
import com.ttthinh.shoe_shop_basic.service.ProductVariantService;
import com.ttthinh.shoe_shop_basic.service.image.ImageUploadQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ProductVariantMapper productVariantMapper;
    private final ImageUploadQueueService imageUploadQueueService;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "variants", allEntries = true),
            @CacheEvict(value = "variantsByProduct", key = "#result.productId")
    })
    public ProductVariantResponse addVariantImages(List<MultipartFile> images, String variantId, Integer primaryIndex) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(()-> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
  //      String imagePath = "products/"+variant.getProduct().getName()+"/images/";
        if (images == null || images.isEmpty()) {
            return productVariantMapper.toProductVariantResponse(variant);
        }
        imageUploadQueueService.enqueueVariantImages(
                images,
                variant.getProduct().getId(),
                variant.getId(),
                primaryIndex
        );

        return productVariantMapper.toProductVariantResponse(
                variant
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "variants", key = "#id")
    public ProductVariantResponse getProductVariant(String id) {
        ProductVariant productVariant = variantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        return productVariantMapper.toProductVariantResponse(productVariant);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "variants", key = "'all'")
    public List<ProductVariantResponse> getAllProductVariant() {
        return productVariantMapper.toProductVariantsResponse(variantRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "variantsByProduct", key = "#productId")
    public List<ProductVariantResponse> getProductVariantByProduct(String productId) {
        return productVariantMapper.toProductVariantsResponse(variantRepository.findByProductId(productId));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "variants", allEntries = true),
            @CacheEvict(value = "variantsByProduct", key = "#result.productId")
    })
    public ProductVariantResponse addProductVariant(ProductVariantRequest productVariantRequest) {
        Product product = productRepository.findById(productVariantRequest.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        ProductVariant productVariant = ProductVariant.builder()
                .product(product)
                .color(productVariantRequest.getColor())
                .active(productVariantRequest.getActive() != null ? productVariantRequest.getActive() : true)
                .build();

        ProductVariant savedVariant = variantRepository.save(productVariant);
        addSizesToVariant(savedVariant, productVariantRequest.getSizes());

        return productVariantMapper.toProductVariantResponse(variantRepository.save(savedVariant));
    }
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "variants", allEntries = true),
            @CacheEvict(value = "variantsByProduct", allEntries = true)
    })
    public List<ProductVariantResponse> addProductVariants(
            List<ProductVariantRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        // Lấy product từ request đầu tiên
        String productId = requests.get(0).getProductId();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        List<ProductVariant> variants = new ArrayList<>();

        for (ProductVariantRequest req : requests) {

            // Optional: validate cùng productId
            if (!productId.equals(req.getProductId())) {
                throw new AppException(ErrorCode.INVALID_PRODUCT_VARIANT_REQUEST);
            }

            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .color(req.getColor())
                    .active(req.getActive() != null ? req.getActive() : true)
                    .build();

            addSizesToVariant(variant, req.getSizes());

            variants.add(variant);
        }

      // return productVariantMapper.toProductVariantsResponse(variantRepository.saveAll(variants));
        return variantRepository.saveAll(variants)
                .stream()
                .map(productVariantMapper::toProductVariantResponse)
                .toList();
    }


    @Override
    public ProductVariantResponse updateProductVariant(String id, ProductVariantRequest productVariantRequest) {
        return null;
    }

    @Transactional
    public void setPrimaryVariantImage(String variantId, String imageId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        variant.getImages().forEach(img ->
                img.setPrimaryImage(img.getId().equals(imageId))
        );
    }

    private void addSizesToVariant(
            ProductVariant variant,
            List<VariantSizeRequest> sizeRequests
    ) {
        if (sizeRequests == null || sizeRequests.isEmpty()) {
            return;
        }

        for (VariantSizeRequest request : sizeRequests) {
            VariantSize variantSize = VariantSize.builder()
                    .variant(variant)
                    .size(request.getSize())
                    .sku(request.getSku())
                    .price(request.getPrice())
                    .build();

            Integer quantity = request.getInitQuantity();
            if (quantity != null && quantity >= 0) {
                Inventory inventory = Inventory.builder()
                        .variantSize(variantSize)
                        .quantity(quantity)
                        .build();
                variantSize.setInventory(inventory);
            }

            variant.getSizes().add(variantSize);
        }
    }

}
