package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.dto.request.catalog.BrandRequest;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.BrandResponse;
import com.ttthinh.shoe_shop_basic.dto.response.common.PageResponse;
import com.ttthinh.shoe_shop_basic.entity.catalog.Brand;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.BrandMapper;
import com.ttthinh.shoe_shop_basic.repository.jpa.BrandRepository;
import com.ttthinh.shoe_shop_basic.service.BrandService;
import com.ttthinh.shoe_shop_basic.service.image.ImageUploadQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {
    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;
    private final ImageUploadQueueService imageUploadQueueService;
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
    public PageResponse<BrandResponse> getBrandPage(int page, int size) {
        Page<Brand> brandPage = brandRepository.findAll(PageRequest.of(page, size));
        return PageResponse.<BrandResponse>builder()
                .items(brandMapper.toBrandResponse(brandPage.getContent()))
                .page(brandPage.getNumber())
                .size(brandPage.getSize())
                .totalItems(brandPage.getTotalElements())
                .totalPages(brandPage.getTotalPages())
                .first(brandPage.isFirst())
                .last(brandPage.isLast())
                .build();
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
        if(image.isEmpty()){
            return brandMapper.toBrandResponse(brandRepository.save(brand));
        }
        imageUploadQueueService.enqueueBrandLogo(image, brand.getId());
        return brandMapper.toBrandResponse(brand);
    }

    @Override
    public BrandResponse addBrandWithImage(MultipartFile image, BrandRequest brandRequest) {
        Brand brand = create(brandRequest);
        if(image == null || image.isEmpty()){
            return brandMapper.toBrandResponse(brandRepository.save(brand));
        }
        imageUploadQueueService.enqueueBrandLogo(image, brand.getId());
        return brandMapper.toBrandResponse(brand);
    }

    @Override
    public BrandResponse getBrandById(String brandId) {
        return brandMapper.toBrandResponse(brandRepository.findById(brandId).orElseThrow(
                () -> new AppException(ErrorCode.BRAND_NOT_FOUND)
        ));
    }
}
