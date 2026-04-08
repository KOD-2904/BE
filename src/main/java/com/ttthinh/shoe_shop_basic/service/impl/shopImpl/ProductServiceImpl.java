package com.ttthinh.shoe_shop_basic.service.impl.shopImpl;

import com.ttthinh.shoe_shop_basic.dto.request.shop.ProductRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shop.ProductResponse;
import com.ttthinh.shoe_shop_basic.entity.Brand;
import com.ttthinh.shoe_shop_basic.entity.Category;
import com.ttthinh.shoe_shop_basic.entity.product.Product;
import com.ttthinh.shoe_shop_basic.entity.product.ProductImage;
import com.ttthinh.shoe_shop_basic.enums.ProductStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.ProductMapper;
import com.ttthinh.shoe_shop_basic.repository.shop.BrandRepository;
import com.ttthinh.shoe_shop_basic.repository.shop.CategoryRepository;
import com.ttthinh.shoe_shop_basic.repository.shop.ProductImageRepository;
import com.ttthinh.shoe_shop_basic.repository.shop.ProductRepository;
import com.ttthinh.shoe_shop_basic.service.CloudinaryService;
import com.ttthinh.shoe_shop_basic.service.shop.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;
    private final ProductImageRepository productImageRepository;
    @Override
    public Product addProduct(ProductRequest productRequest) {
        Brand brand = brandRepository.findById(productRequest.getBrandId())
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
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
        for (int i = 0; i < images.size(); i++) {

            boolean isPrimary =
                    primaryIndex != null && primaryIndex == i;

            String imageUrl = cloudinaryService.upload(
                    images.get(i),
                    "products/" + product.getId()
            );
            ProductImage image = ProductImage.builder()
                    .product(product)
                    .url(imageUrl)
                    .primaryImage(isPrimary)
                    .sortOrder(i)
                    .build();

            productImageRepository.save(image);
        }
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

        Set<ProductImage> productImages = product.getImages();

        // Nếu có primaryIndex → reset primary cũ
        if (primaryIndex != null) {
            productImages.forEach(img -> img.setPrimaryImage(false));
        }

        int startSortOrder = productImages.size();

        for (int i = 0; i < images.size(); i++) {
            boolean isPrimary = primaryIndex != null && primaryIndex == i;

            String imageUrl = cloudinaryService.upload(
                    images.get(i),
                    "products/" + product.getId()
            );

            ProductImage image = ProductImage.builder()
                    .product(product)
                    .url(imageUrl)
                    .primaryImage(isPrimary)
                    .sortOrder(startSortOrder + i)
                    .build();

            productImages.add(image);
        }

        product.setImages(productImages);
        productRepository.save(product); // cascade ALL → auto save ProductImage

        return productMapper.toProductResponse(product);
    }

    @Override
    public ProductResponse getProduct(String productId) {
        return productMapper.toProductResponse(productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND)
                ));
    }

}
