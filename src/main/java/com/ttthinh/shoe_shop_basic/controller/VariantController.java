package com.ttthinh.shoe_shop_basic.controller;

import com.ttthinh.shoe_shop_basic.dto.request.shop.ProductVariantRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.shop.ProductVariantResponse;
import com.ttthinh.shoe_shop_basic.entity.product.ProductVariant;
import com.ttthinh.shoe_shop_basic.repository.shop.ProductVariantRepository;
import com.ttthinh.shoe_shop_basic.service.shop.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/variants")
public class VariantController {
    private final ProductVariantService productVariantService;

    @PostMapping("/add")
    public ApiResponse<ProductVariantResponse> add(@RequestBody ProductVariantRequest request,
                                                   @RequestParam(required = false) Integer initQuantity) {
        var result = productVariantService.addProductVariant(request, initQuantity);
        return ApiResponse.<ProductVariantResponse>builder()
                .result(result)
                .code(200)
                .message("success")
                .build();
    }
    @PostMapping("/add")
    public ApiResponse<List<ProductVariantResponse>> add(@RequestBody List<ProductVariantRequest> request,
                                                   @RequestParam(required = false) Integer initQuantity) {
        var result = productVariantService.addProductVariants(request, initQuantity);
        return ApiResponse.<List<ProductVariantResponse>>builder()
                .result(result)
                .code(200)
                .message("success")
                .build();
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/add-images")
    public ApiResponse<ProductVariantResponse> addVariantImages(
            @RequestPart(value = "images") List<MultipartFile> images,
            @RequestParam(value = "variantId") String variantId,
            @RequestParam(value = "primaryIndex") Integer primaryIndex
    ){
        var result = productVariantService.addVariantImages(images, variantId, primaryIndex);
        return ApiResponse.<ProductVariantResponse>builder()
                .result(result)
                .code(200)
                .message("success")
                .build();
    }
}
