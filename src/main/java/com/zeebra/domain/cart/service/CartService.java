package com.zeebra.domain.cart.service;

import com.zeebra.domain.cart.dto.CartRequest;
import com.zeebra.domain.cart.dto.CartResponse;

public interface CartService {

    public CartResponse addCartItem(Long memberId, Long productOptionId, CartRequest request);

}

