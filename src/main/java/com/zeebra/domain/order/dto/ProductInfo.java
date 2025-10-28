package com.zeebra.domain.order.dto;

import java.util.List;

public record ProductInfo(
	Long saleId,
	Long productOptionId,
	String productName,
	String productThumbnail,
	List<OrderOption> orderItemOptions
) {
}