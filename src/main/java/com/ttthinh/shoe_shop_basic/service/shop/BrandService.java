package com.ttthinh.shoe_shop_basic.service.shop;

import com.ttthinh.shoe_shop_basic.dto.request.shop.BrandRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shop.BrandResponse;
import com.ttthinh.shoe_shop_basic.entity.Brand;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BrandService {
    public Brand create(BrandRequest brand);
    public BrandResponse update(String id, BrandRequest brand);
    public List<BrandResponse> getAllBrands();

    public BrandResponse addBrandImage(MultipartFile image, String brandId);

    public BrandResponse addBrandWithImage(MultipartFile image, BrandRequest brandRequest);

     public BrandResponse getBrandById(String brandId);
}
