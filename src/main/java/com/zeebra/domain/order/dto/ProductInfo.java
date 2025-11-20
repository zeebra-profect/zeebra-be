package com.zeebra.domain.order.dto;

import java.util.List;

public record ProductInfo(
	Long saleId,
	Long productOptionId,
	String productName,
	String productThumbnail,
	List<OrderOption> orderItemOptions
) {
	public static ProductInfo of(
		Long saleId,
		Long productOptionId,
		String productName,
		String productThumbnail,
		List<OrderOption> orderItemOptions
	) {
		return new ProductInfo(
			saleId,
			productOptionId,
			productName,
			productThumbnail,
			orderItemOptions
		);
	}
}