package com.ttthinh.shoe_shop_basic.controller;

import com.ttthinh.shoe_shop_basic.dto.request.shop.CategoryRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.shop.CategoryResponse;
import com.ttthinh.shoe_shop_basic.service.shop.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<CategoryResponse> add(@RequestBody CategoryRequest request) {
        return ApiResponse.<CategoryResponse>
                builder()
                .result(categoryService.createCategory(request))
                .code(200)
                .message("Success")
                .build();
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/getCategorys")
    public ApiResponse<List<CategoryResponse>> getCategorys() {
        return ApiResponse.<List<CategoryResponse>>
                        builder()
                .result(categoryService.getAllCategories())
                .code(200)
                .message("Success")
                .build();
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/getCategory")
    public ApiResponse<CategoryResponse> getCategory(@RequestParam("id") String id) {
        return ApiResponse.<CategoryResponse>builder()
                .code(200)
                .message("Success")
                .result(categoryService.getCategory(id))
                .build();
    }
}
