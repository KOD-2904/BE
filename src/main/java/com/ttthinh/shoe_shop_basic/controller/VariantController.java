package com.ttthinh.shoe_shop_basic.controller;

import com.ttthinh.shoe_shop_basic.dto.request.shop.ProductVariantRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.shop.ProductVariantResponse;
import com.ttthinh.shoe_shop_basic.entity.product.ProductVariant;
import com.ttthinh.shoe_shop_basic.repository.shop.ProductVariantRepository;
import com.ttthinh.shoe_shop_basic.service.shop.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/variants")
public class VariantController {
    private final ProductVariantService productVariantService;

    @PostMapping("/addVariant")
    public ApiResponse<ProductVariantResponse> addVariant(@RequestBody ProductVariantRequest request,
                                                   @RequestParam(required = false) Integer initQuantity) {
        var result = productVariantService.addProductVariant(request, initQuantity);
        return ApiResponse.<ProductVariantResponse>builder()
                .result(result)
                .code(200)
                .message("success")
                .build();
    }
    @PostMapping("/addVariants")
    public ApiResponse<List<ProductVariantResponse>> addVariants(
            @RequestBody List<ProductVariantRequest> request,
            @RequestParam(value = "init", required = false) Integer initQuantity) {
        var result = productVariantService.addProductVariants(request, initQuantity);
        return ApiResponse.<List<ProductVariantResponse>>builder()
                .result(result)
                .code(200)
                .message("success")
                .build();
    }
    @PostMapping(
            value = "/variants/{variantId}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<ProductVariantResponse> addVariantImages(
            @PathVariable String variantId,
            @RequestPart("images") List<MultipartFile> images,
            @RequestParam(required = false) Integer primaryIndex
    ) {
        return ApiResponse.<ProductVariantResponse>builder()
                .result(
                        productVariantService.addVariantImages(
                                images,
                                variantId,
                                primaryIndex
                        )
                )
                .message("success")
                .code(200)
                .build();
    }

}
