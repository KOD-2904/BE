package com.ttthinh.shoe_shop_basic.controller;

import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.shop.InventoryResponse;
import com.ttthinh.shoe_shop_basic.entity.product.Inventory;
import com.ttthinh.shoe_shop_basic.service.shop.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("/{variantId}")
    public ApiResponse<InventoryResponse> getInventory(@PathVariable String variantId) {
        return ApiResponse.<InventoryResponse>builder()
                .result(inventoryService.getInventory(variantId))
                .code(200)
                .message("success")
                .build();
    }
    @PostMapping("/increase")
    public ApiResponse<InventoryResponse> increase(
            @RequestParam String variantId,
            @RequestParam int quantity
    ) {

        return ApiResponse.<InventoryResponse>builder()
                .result(inventoryService.increaseStock(variantId, quantity))
                .code(200)
                .message("success")
                .build();
    }

    @PostMapping("/decrease")
    public ApiResponse<InventoryResponse> decrease(
            @RequestParam String variantId,
            @RequestParam int quantity
    ) {

        return ApiResponse.<InventoryResponse>builder()
                .result(inventoryService.decreaseStock(variantId, quantity))
                .code(200)
                .message("success")
                .build();
    }

    @PutMapping("/set")
    public ApiResponse<InventoryResponse> set(
            @RequestParam String variantId,
            @RequestParam int quantity
    ) {

        return ApiResponse.<InventoryResponse>builder()
                .result(inventoryService.setStock(variantId, quantity))
                .code(200)
                .message("success")
                .build();
    }
}
