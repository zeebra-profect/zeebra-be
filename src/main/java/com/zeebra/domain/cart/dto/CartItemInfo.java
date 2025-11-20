package com.zeebra.domain.cart.dto;

import java.math.BigDecimal;
import java.util.List;

import com.zeebra.domain.cart.entity.CartItem;

import lombok.Builder;

public record CartItemInfo(
	Long cartItemId,
	Long productOptionId,
	BigDecimal snapShotPrice,
	int quantity
) {
	@Builder
	public CartItemInfo(Long cartItemId,
		Long productOptionId,
		BigDecimal snapShotPrice,
		int quantity){
		this.cartItemId = cartItemId;
		this.productOptionId = productOptionId;
		this.snapShotPrice = snapShotPrice;
		this.quantity = quantity;
	}
	public static CartItemInfo of(CartItem cartItem){
		return CartItemInfo.builder()
			.cartItemId(cartItem.getId())
			.productOptionId(cartItem.getProductOptionId())
			.snapShotPrice(cartItem.getSnapShotPrice())
			.quantity(cartItem.getQuantity())
			.build();
	}

	public static List<CartItemInfo> of(List<CartItem> items) {
		return items.stream()
			.map(CartItemInfo::of)
			.toList();
	}
}