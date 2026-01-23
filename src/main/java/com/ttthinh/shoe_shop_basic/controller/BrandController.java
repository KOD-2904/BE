package com.ttthinh.shoe_shop_basic.controller;

import com.ttthinh.shoe_shop_basic.dto.request.shop.BrandRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.shop.BrandResponse;
import com.ttthinh.shoe_shop_basic.mapper.BrandMapper;
import com.ttthinh.shoe_shop_basic.service.shop.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;
    private final BrandMapper brandMapper;
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<BrandResponse> addBrand(@RequestBody BrandRequest brandRequest) {
        var result = brandMapper.toBrandResponse(brandService.create(brandRequest));
        return ApiResponse.<BrandResponse>builder()
                .result(result)
                .code(200)
                .message("Success")
                .build();
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping(value = "/addBrandImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BrandResponse> addBrandImage(
            @RequestPart(value = "image") MultipartFile image,
            @RequestParam(value = "brandId") String brandId) {
        var result = brandService.addBrandImage(image, brandId);
        return ApiResponse.<BrandResponse>builder()
                .result(result)
                .code(200)
                .message("Success")
                .build();
    }
    @PostMapping(value = "/addBrandWithImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<BrandResponse> addBrandWithImage(
            @RequestPart(value = "image", required = true) MultipartFile image,
            @RequestPart(value = "brand", required = true) BrandRequest brandRequest) {
        var result = brandService.addBrandWithImage(image, brandRequest);
        return ApiResponse.<BrandResponse>builder()
                .result(result)
                .code(200)
                .message("Success")
                .build();
    }
}
