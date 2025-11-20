package com.zeebra.domain.order.dto;

import java.util.List;

import lombok.Builder;

public record ProductInfo(
	Long saleId,
	Long productOptionId,
	String productName,
	String productThumbnail,
	List<OrderOption> orderItemOptions
) {
	@Builder
	public ProductInfo(Long saleId, Long productOptionId, String productName, String productThumbnail, List<OrderOption> orderItemOptions) {
		this.saleId = saleId;
		this.productOptionId = productOptionId;
		this.productName = productName;
		this.productThumbnail = productThumbnail;
		this.orderItemOptions = orderItemOptions;
	}
	public static ProductInfo of(Long saleId, Long productOptionId, String productName, String productThumbnail, List<OrderOption> orderItemOptions) {
		return ProductInfo.builder()
			.saleId(saleId)
			.productOptionId(productOptionId)
			.productName(productName)
			.productThumbnail(productThumbnail)
			.orderItemOptions(orderItemOptions)
			.build();
	}
}