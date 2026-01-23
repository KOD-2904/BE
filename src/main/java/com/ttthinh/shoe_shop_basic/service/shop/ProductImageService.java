package com.ttthinh.shoe_shop_basic.service.shop;

import com.ttthinh.shoe_shop_basic.entity.product.ProductImage;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImageService {
    public ProductImage addImage(String productId, MultipartFile file, boolean primary);
}
