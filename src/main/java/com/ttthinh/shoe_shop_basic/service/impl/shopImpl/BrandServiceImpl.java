package com.ttthinh.shoe_shop_basic.service.impl.shopImpl;

import com.ttthinh.shoe_shop_basic.dto.request.shop.BrandRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shop.BrandResponse;
import com.ttthinh.shoe_shop_basic.entity.Brand;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.BrandMapper;
import com.ttthinh.shoe_shop_basic.repository.shop.BrandRepository;
import com.ttthinh.shoe_shop_basic.service.CloudinaryService;
import com.ttthinh.shoe_shop_basic.service.shop.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {
    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;
    private final CloudinaryService cloudinaryService;
    @Override
    public Brand create(BrandRequest brandRequest) {
        Brand brand = new Brand();
        brand.setName(brandRequest.getName());
        brand.setLogoUrl(brandRequest.getLogoUrl());
        return brandRepository.save(brand);
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

    @Override
    public BrandResponse addBrandImage(MultipartFile image, String brandId) {
        Brand brand = brandRepository.findById(brandId).orElseThrow(
                () -> new AppException(ErrorCode.BRAND_NOT_FOUND)
        );
        if(!image.isEmpty()){
            return brandMapper.toBrandResponse(brandRepository.save(brand));
        }
        String brandUrl = cloudinaryService.upload(
                image, "brand/" + brand.getId());
        brand.setLogoUrl(brandUrl);
        return brandMapper.toBrandResponse(brandRepository.save(brand));
    }

    @Override
    public BrandResponse addBrandWithImage(MultipartFile image, BrandRequest brandRequest) {
        Brand brand = create(brandRequest);
        if(image == null || !image.isEmpty()){
            return brandMapper.toBrandResponse(brandRepository.save(brand));
        }
        String brandUrl = cloudinaryService.upload(image, "brand/" + brand.getId());
        brand.setLogoUrl(brandUrl);

        return brandMapper.toBrandResponse(brandRepository.save(brand));
    }
}
