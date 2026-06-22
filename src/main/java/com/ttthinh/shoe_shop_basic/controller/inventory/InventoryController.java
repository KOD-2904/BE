package com.ttthinh.shoe_shop_basic.controller.inventory;

import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.inventory.InventoryResponse;
import com.ttthinh.shoe_shop_basic.entity.inventory.Inventory;
import com.ttthinh.shoe_shop_basic.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("/{variantSizeId}")
    public ApiResponse<InventoryResponse> getInventory(@PathVariable String variantSizeId) {
        return ApiResponse.<InventoryResponse>builder()
                .result(inventoryService.getInventory(variantSizeId))
                .code(200)
                .message("success")
                .build();
    }
    @PostMapping("/increase")
    public ApiResponse<InventoryResponse> increase(
            @RequestParam String variantSizeId,
            @RequestParam int quantity
    ) {

        return ApiResponse.<InventoryResponse>builder()
                .result(inventoryService.increaseStock(variantSizeId, quantity))
                .code(200)
                .message("success")
                .build();
    }

    @PostMapping("/decrease")
    public ApiResponse<InventoryResponse> decrease(
            @RequestParam String variantSizeId,
            @RequestParam int quantity
    ) {

        return ApiResponse.<InventoryResponse>builder()
                .result(inventoryService.decreaseStock(variantSizeId, quantity))
                .code(200)
                .message("success")
                .build();
    }

    @PutMapping("/set")
    public ApiResponse<InventoryResponse> set(
            @RequestParam String variantSizeId,
            @RequestParam int quantity
    ) {

        return ApiResponse.<InventoryResponse>builder()
                .result(inventoryService.setStock(variantSizeId, quantity))
                .code(200)
                .message("success")
                .build();
    }
}
