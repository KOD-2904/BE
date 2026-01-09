package com.ttthinh.shoe_shop_basic.service.impl.shopImpl;

import com.ttthinh.shoe_shop_basic.dto.request.shop.BrandRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shop.BrandResponse;
import com.ttthinh.shoe_shop_basic.entity.Brand;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.BrandMapper;
import com.ttthinh.shoe_shop_basic.repository.shop.BrandRepository;
import com.ttthinh.shoe_shop_basic.service.shop.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {
    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;
    @Override
    public BrandResponse create(BrandRequest brandRequest) {
        Brand brand = new Brand();
        brand.setName(brandRequest.getName());
        brand.setLogoUrl(brandRequest.getLogoUrl());
        return brandMapper.toBrandResponse(brandRepository.save(brand));
    }

    @Override
    public BrandResponse update(String id, BrandRequest brandRequest) {
        Brand brand = brandRepository.findById(id).orElseThrow(
                () -> new AppException(ErrorCode.BRAND_NOT_FOUND)
        );
        brand.setName(brandRequest.getName());
        brand.setLogoUrl(brandRequest.getLogoUrl());
        return brandMapper.toBrandResponse(brandRepository.save(brand));
    }

    @Override
    public List<BrandResponse> getAllBrands() {
        return brandMapper.toBrandResponse(brandRepository.findAll());
    }
}
