package com.ttthinh.shoe_shop_basic.service.shop;

import com.ttthinh.shoe_shop_basic.dto.request.shop.BrandRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shop.BrandResponse;
import com.ttthinh.shoe_shop_basic.entity.Brand;

import java.util.List;

public interface BrandService {
    public BrandResponse create(BrandRequest brand);
    public BrandResponse update(String id, BrandRequest brand);
    public List<BrandResponse> getAllBrands();
}
