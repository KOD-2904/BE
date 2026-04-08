package com.ttthinh.shoe_shop_basic.controller;

import com.ttthinh.shoe_shop_basic.dto.request.shop.ProductRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.shop.ProductResponse;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.ProductMapper;
import com.ttthinh.shoe_shop_basic.service.shop.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.util.List;


@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ProductMapper productMapper;
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    @PostMapping(value = "/with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ApiResponse<ProductResponse> crateProducts(
//            @RequestParam("product")String productRequest,
//            @RequestPart(value = "images", required = false) List<MultipartFile> images,
//            @RequestParam(value = "primaryIndex", required = false) Integer primaryIndex) {
//        ObjectMapper mapper = new ObjectMapper();
//        ProductRequest request;
//        try {
//            request = mapper.readValue(productRequest, ProductRequest.class);
//        } catch (Exception e) {
//            throw new AppException(ErrorCode.VALIDATION_ERROR);
//        }
//        return ApiResponse.<ProductResponse>builder()
//                .result(productService.createProductWithImage(
//                        request, images, primaryIndex
//                ))
//                .code(200)
//                .message("success")
//                .build();
//    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping(value = "/with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProductResponse> creatProduct(
            @RequestPart("product")ProductRequest productRequest,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "primaryIndex", required = false) Integer primaryIndex) {

        return ApiResponse.<ProductResponse>builder()
                .result(productService.createProductWithImage(
                        productRequest, images, primaryIndex
                ))
                .code(200)
                .message("success")
                .build();
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping(value = "/")
    public ApiResponse<ProductResponse> createProduct(
            @RequestBody ProductRequest productRequest) {
        return ApiResponse.<ProductResponse>builder()
                .result(productMapper.toProductResponse(productService.addProduct(
                        productRequest)
                ))
                .code(200)
                .message("success")
                .build();
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping(value = "/addProductImage")
    public ApiResponse<ProductResponse> addProductImage(
            @RequestPart(value = "images") List<MultipartFile> images,
            @RequestParam(value = "productId")String productId,
            @RequestParam(value = "primaryIndex")Integer primaryIndex
    ){
        var result = ApiResponse.<ProductResponse>builder()
                .message("success")
                .code(200)
                .result(productService.addProductImages(images, productId, primaryIndex))
                .build();
        return null;
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping(value = "/getProducts")
    public ApiResponse<List<ProductResponse>> getProducts(){
        return ApiResponse.<List<ProductResponse>>builder()
                .message("success")
                .code(200)
                .result(productService.getAllProducts())
                .build();
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping(value = "/getProduct")
    public ApiResponse<ProductResponse> getProducts(@RequestParam String productId){
        return ApiResponse.<ProductResponse>builder()
                .message("success")
                .code(200)
                .result(productService.getProduct(productId))
                .build();
    }
}
