package com.ttthinh.shoe_shop_basic.service.shop;

import com.ttthinh.shoe_shop_basic.dto.request.shop.AddCartItemRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shop.CartItemResponse;
import com.ttthinh.shoe_shop_basic.dto.response.shop.CartResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;

public interface CartService {
    CartResponse getMyCart(UserAccount user);

    CartItemResponse addItem(UserAccount user, AddCartItemRequest request);

    CartItemResponse updateItem(UserAccount user, String cartItemId, int quantity);

    CartItemResponse removeItem(UserAccount userId, String cartItemId);

    void clearCart(UserAccount user);

}
