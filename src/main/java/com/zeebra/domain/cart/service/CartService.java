package com.zeebra.domain.cart.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.zeebra.domain.cart.dto.CartItemInfo;
import com.zeebra.domain.cart.dto.CartRequest;
import com.zeebra.domain.cart.dto.CartResponse;
import com.zeebra.domain.cart.dto.GetCartResponse;
import com.zeebra.global.ApiResponse;

public interface CartService {

    public ApiResponse<CartResponse> addCartItem(Long memberId, Long productOptionId, CartRequest request);

    public ApiResponse<Void> deleteCartItem(Long memberId, Long productOptionId);

	GetCartResponse getCartItems(Long memberId, Pageable pageable);

	List<CartItemInfo> getCartItemsByCartId(Long cartId, Long memberId);
}