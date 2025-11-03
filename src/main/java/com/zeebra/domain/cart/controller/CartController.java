package com.zeebra.domain.cart.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.zeebra.domain.cart.dto.CartRequest;
import com.zeebra.domain.cart.dto.CartResponse;
import com.zeebra.domain.cart.dto.GetCartResponse;
import com.zeebra.domain.cart.service.CartService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart API", description = "장바구니 관련 API")
public class CartController {

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

	@Operation(summary = "장바구니 조회 API")
	@GetMapping("/api/carts")
	public ApiResponse<GetCartResponse> getCart(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal,
		@ParameterObject @PageableDefault(size = 100, sort = "createdTime",	direction = Sort.Direction.DESC) Pageable pageable) {

		Long memberId = principal.getMemberId();

		return ApiResponse.success(cartService.getCartItems(memberId, pageable));
	}
}