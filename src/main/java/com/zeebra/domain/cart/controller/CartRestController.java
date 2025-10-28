package com.zeebra.domain.cart.controller;

import com.zeebra.domain.cart.dto.CartRequest;
import com.zeebra.domain.cart.dto.CartResponse;
import com.zeebra.domain.cart.service.CartService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CartRestController {

    private final CartService cartService;

    @PostMapping("/api/carts/{productOptionId}")
    public ApiResponse<CartResponse> addCartItem(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal,
                                                 @PathVariable Long productOptionId, @RequestBody CartRequest request) {
        Long memberId = principal.getMemberId();
        return cartService.addCartItem(memberId, productOptionId, request);
    }

    @DeleteMapping("/api/carts/{cartItemId}")
    public ApiResponse<Void> deleteCartItem(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal,
                                            @PathVariable Long productOptionId) {
        Long memberId = principal.getMemberId();
        return cartService.deleteCartItem(memberId, productOptionId);
    }
}
