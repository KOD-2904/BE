package com.ttthinh.shoe_shop_basic.service.impl.shopImpl;

import com.ttthinh.shoe_shop_basic.dto.request.shop.AddCartItemRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shop.CartItemResponse;
import com.ttthinh.shoe_shop_basic.dto.response.shop.CartResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.cart.Cart;
import com.ttthinh.shoe_shop_basic.entity.cart.CartItem;
import com.ttthinh.shoe_shop_basic.entity.product.Inventory;
import com.ttthinh.shoe_shop_basic.entity.product.ProductVariant;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.CartItemMapper;
import com.ttthinh.shoe_shop_basic.repository.auth.UserAccountRepository;
import com.ttthinh.shoe_shop_basic.repository.shop.*;
import com.ttthinh.shoe_shop_basic.service.shop.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final CartItemMapper cartItemMapper;

    @Transactional
    @Override
    public CartResponse getMyCart(UserAccount user) {
        log.warn("UserId = {}", user.getId());

        Cart cart = getOrCreateCart(user);
        List<CartItem> items = cartItemRepository.findByCart(cart);

        List<CartItemResponse> itemResponses = cartItemMapper.toResponse(items);

        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(itemResponses)
                .subtotal(subtotal)
                .build();
    }

    private Cart getOrCreateCart(UserAccount user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }


    @Override
    @Transactional
    public CartItemResponse addItem(UserAccount user, AddCartItemRequest request) {

        Cart cart = getOrCreateCart(user);

        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        Inventory inventory = inventoryRepository.findByVariant(variant)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        if (inventory.getQuantity() < request.getQuantity()) {
            throw new AppException(ErrorCode.OUT_OF_STOCK);
        }

        CartItem item = cartItemRepository
                .findByCartAndVariant(cart, variant)
                .orElse(null);

        if (item != null) {
            int newQty = item.getQuantity() + request.getQuantity();
            if (newQty > inventory.getQuantity()) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }
            item.setQuantity(newQty);
        } else {
            item = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .build();
        }

        return cartItemMapper.toResponse(cartItemRepository.save(item));
    }

    @Override
    @Transactional
    public CartItemResponse updateItem(UserAccount user, String cartItemId, int quantity) {

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        // quantity <= 0 → remove item
        if (quantity <= 0) {
            CartItemResponse response = cartItemMapper.toResponse(item);
            cartItemRepository.delete(item);
            return response;
        }

        Inventory inventory = inventoryRepository.findByVariant(item.getVariant())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        if (quantity > inventory.getQuantity()) {
            throw new AppException(ErrorCode.OUT_OF_STOCK);
        }

        item.setQuantity(quantity);
        return cartItemMapper.toResponse(item);
    }


    @Override
    @Transactional
    public CartItemResponse removeItem(UserAccount user, String cartItemId) {

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        CartItemResponse response = cartItemMapper.toResponse(item);
        cartItemRepository.delete(item);

        return response;
    }


    @Override
    public void clearCart(UserAccount user) {
        Cart cart = getOrCreateCart(user);

        List<CartItem> items = cartItemRepository.findByCart(cart);

        cartItemRepository.deleteAll(items);
    }
}
