package com.ttthinh.shoe_shop_basic.service.impl.shopImpl;

import com.ttthinh.shoe_shop_basic.dto.request.shop.ProductVariantRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shop.ProductVariantResponse;
import com.ttthinh.shoe_shop_basic.entity.product.Inventory;
import com.ttthinh.shoe_shop_basic.entity.product.Product;
import com.ttthinh.shoe_shop_basic.entity.product.ProductVariant;
import com.ttthinh.shoe_shop_basic.entity.product.VariantImage;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.ProductVariantMapper;
import com.ttthinh.shoe_shop_basic.repository.shop.ProductRepository;
import com.ttthinh.shoe_shop_basic.repository.shop.ProductVariantRepository;
import com.ttthinh.shoe_shop_basic.service.CloudinaryService;
import com.ttthinh.shoe_shop_basic.service.shop.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ProductVariantMapper productVariantMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    public ProductVariantResponse addVariantImages(List<MultipartFile> images, String variantId, Integer primaryIndex) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(()-> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
  //      String imagePath = "products/"+variant.getProduct().getName()+"/images/";
        if (images == null || images.isEmpty()) {
            return productVariantMapper.toProductVariantResponse(variant);
        }
        Set<VariantImage> variantImages = variant.getImages();
        if (primaryIndex != null) {
            variantImages.forEach(img -> img.setPrimaryImage(false));
        }
        for (int i = 0; i < images.size(); i++) {
            boolean isPrimary = primaryIndex != null && i == primaryIndex;

            String imageUrl = cloudinaryService.upload(
                    images.get(i),
                    "products/" + variant.getProduct().getId()
                            + "/" + variant.getId()
            );

            VariantImage image = VariantImage.builder()
                    .variant(variant)
                    .url(imageUrl)
                    .primaryImage(isPrimary)
                    .sortOrder(i)
                    .build();

            variantImages.add(image);
        }

        return productVariantMapper.toProductVariantResponse(
                variantRepository.save(variant)
        );
    }

    @Override
    public ProductVariantResponse getProductVariant(String id) {
        ProductVariant productVariant = variantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        return productVariantMapper.toProductVariantResponse(productVariant);
    }

    @Override
    public List<ProductVariantResponse> getAllProductVariant() {
        //return productVariantMapper.toProductVariantResponse(variantRepository.findAll());
        return null;
    }

    @Override
    public List<ProductVariantResponse> getProductVariantByProduct(String product) {
        return List.of();
    }

    @Override
    public ProductVariantResponse addProductVariant(ProductVariantRequest productVariantRequest, Integer initQuantity) {
        Product product = productRepository.findById(productVariantRequest.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        ProductVariant productVariant = ProductVariant.builder()
                .product(product)
                .sku(productVariantRequest.getSku())
                .active(true)
                .color(productVariantRequest.getColor())
                .price(productVariantRequest.getPrice())
                .size(productVariantRequest.getSize())
                .build();
        //Inventory inventoryVariant = productVariant.getInventory();
        if (initQuantity != null && initQuantity > 0) {
            Inventory inventory = Inventory.builder()
                    .variant(productVariant)
                    .quantity(initQuantity)
                    .build();
            productVariant.setInventory(inventory);
        }

        return productVariantMapper.toProductVariantResponse(variantRepository.save(productVariant));
    }
    @Override
    @Transactional
    public List<ProductVariantResponse> addProductVariants(
            List<ProductVariantRequest> requests,
            Integer initQuantity
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
                    .sku(req.getSku())
                    .active(true)
                    .color(req.getColor())
                    .size(req.getSize())
                    .price(req.getPrice())
                    .build();

            if (initQuantity != null && initQuantity > 0) {
                Inventory inventory = Inventory.builder()
                        .variant(variant)
                        .quantity(initQuantity)
                        .build();
                variant.setInventory(inventory);
            }

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

}
