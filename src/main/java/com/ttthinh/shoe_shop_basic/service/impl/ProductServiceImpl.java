package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.dto.request.catalog.ProductRequest;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductResponse;
import com.ttthinh.shoe_shop_basic.dto.response.common.PageResponse;
import com.ttthinh.shoe_shop_basic.entity.catalog.Brand;
import com.ttthinh.shoe_shop_basic.entity.catalog.Category;
import com.ttthinh.shoe_shop_basic.entity.catalog.Product;
import com.ttthinh.shoe_shop_basic.enums.ProductStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.ProductMapper;
import com.ttthinh.shoe_shop_basic.repository.jpa.BrandRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.CategoryRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductRepository;
import com.ttthinh.shoe_shop_basic.service.ProductService;
import com.ttthinh.shoe_shop_basic.service.image.ImageUploadQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ImageUploadQueueService imageUploadQueueService;
    @Override
    public Product addProduct(ProductRequest productRequest) {
        Brand brand = null;
        if (productRequest.getBrandId() != null && !productRequest.getBrandId().isBlank()) {
            brand = brandRepository.findById(productRequest.getBrandId())
                    .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
        }

        Category category = null;
        if (productRequest.getCategoryId() != null && !productRequest.getCategoryId().isBlank()) {
            category = categoryRepository.findById(productRequest.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        Product product = Product.builder()
                .brand(brand)
                .category(category)
                .name(productRequest.getName())
                .slug(productRequest.getSlug())
                .description(productRequest.getDescription())
                .basePrice(productRequest.getBasePrice())
                .status(ProductStatus.ACTIVE)
                .build();
        return productRepository.save(product);
        //return productMapper.toProductResponse(productRepository.save(product));
    }

    @Override
    public ProductResponse updateProduct(String id, ProductRequest productRequest) {

        return null;
    }

    @Override
    public PageResponse<ProductResponse> getProductPage(int page, int size) {
        Page<Product> productPage = productRepository.findAll(PageRequest.of(page, size));
        return PageResponse.<ProductResponse>builder()
                .items(productMapper.toProductResponse(productPage.getContent()))
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalItems(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .first(productPage.isFirst())
                .last(productPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productMapper.toProductResponse(productRepository.findAll());
    }

    @Override
    public List<ProductResponse> getProductsByCategory(String category) {
        return List.of();
    }

    @Override
    public List<ProductResponse> getProductsByBrand(String brand) {
        return List.of();
    }

    @Override
    @Transactional
    public ProductResponse createProductWithImage(ProductRequest productRequest, List<MultipartFile> images, Integer primaryIndex) {
        Product product = addProduct(productRequest);
        if (images == null || images.isEmpty()) {
            return productMapper.toProductResponse(product);
        }
        imageUploadQueueService.enqueueProductImages(images, product.getId(), primaryIndex);
        return productMapper.toProductResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse addProductImages(
            List<MultipartFile> images,
            String productId,
            Integer primaryIndex
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (images == null || images.isEmpty()) {
            return productMapper.toProductResponse(product);
        }

        imageUploadQueueService.enqueueProductImages(images, product.getId(), primaryIndex);

        return productMapper.toProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(String productId) {
        return productMapper.toProductResponse(productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND)
                ));
    }

}
