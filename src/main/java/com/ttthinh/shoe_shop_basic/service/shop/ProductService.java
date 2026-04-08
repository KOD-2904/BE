package com.ttthinh.shoe_shop_basic.service.shop;

import com.ttthinh.shoe_shop_basic.dto.request.shop.ProductRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shop.ProductResponse;
import com.ttthinh.shoe_shop_basic.entity.product.Product;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    public Product addProduct(ProductRequest productRequest);
    public ProductResponse updateProduct(String id, ProductRequest productRequest);
    //public ProductResponse updateProductStatus(ProductRequest productRequest);
    public List<ProductResponse> getAllProducts();
    public List<ProductResponse> getProductsByCategory(String category);
    public List<ProductResponse> getProductsByBrand(String brand);
    public ProductResponse createProductWithImage(ProductRequest productRequest,
                                                  List<MultipartFile> images,
                                                  Integer primaryIndex);

    public ProductResponse addProductImages(List<MultipartFile> images, String productId, Integer primaryIndex);

    ProductResponse getProduct(String productId);
}
