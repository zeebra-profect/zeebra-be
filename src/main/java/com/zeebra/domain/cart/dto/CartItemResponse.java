package com.zeebra.domain.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartItemResponse(
	Long productOptionId,
	Long productId,
	BigDecimal snapShotPrice,
	BigDecimal currentLowestPrice,
	int quantity,
	boolean isSoldOut,
	String productName,
	String thumbnail,
	List<CartItemOption> options
) {
}