package com.zeebra.domain.cart.service;

import com.zeebra.domain.cart.dto.CartRequest;
import com.zeebra.domain.cart.dto.CartResponse;
import com.zeebra.global.ApiResponse;

public interface CartService {

    public ApiResponse<CartResponse> addCartItem(Long memberId, Long productOptionId, CartRequest request);

    public ApiResponse<Void> deleteCartItem(Long memberId, Long productOptionId);
}

