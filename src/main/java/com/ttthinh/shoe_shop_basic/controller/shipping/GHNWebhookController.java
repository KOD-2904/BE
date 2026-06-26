package com.ttthinh.shoe_shop_basic.controller.shipping;

import com.ttthinh.shoe_shop_basic.service.shipping.ShippingOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks/ghn")
public class GHNWebhookController {
    private final ShippingOrderService shippingOrderService;

    @PostMapping
    public ResponseEntity<Map<String, String>> handle(@RequestBody Map<String, Object> payload) {
        shippingOrderService.handleGHNWebhook(payload);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }
}
