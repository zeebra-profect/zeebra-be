package com.zeebra.domain.cart.dto;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;

public record GetCartResponse(
	Long cartId,
	BigDecimal totalPrice,
	BigDecimal discount,
	int totalQuantity,
	List<CartItemResponse> cartItems,
	int currentPage,
	int totalPages,
	long totalElements,
	boolean hasNext
) {
	public static GetCartResponse of(Long cartId, BigDecimal totalPrice, BigDecimal discount, int totalQuantity, Page<CartItemResponse> cartItemsPage) {
		return new GetCartResponse(
			cartId,
			totalPrice,
			discount,
			totalQuantity,
			cartItemsPage.getContent(),
			cartItemsPage.getNumber(),
			cartItemsPage.getTotalPages(),
			cartItemsPage.getTotalElements(),
			cartItemsPage.hasNext()
		);
	}
}