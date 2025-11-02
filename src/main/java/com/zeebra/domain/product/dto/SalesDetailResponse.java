package com.zeebra.domain.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.zeebra.domain.product.entity.SalesStatus;

public record SalesDetailResponse(
	Long salesId,
	Long productOptionId,
	BigDecimal price,
	BigDecimal soldPrice,
	SalesStatus salesStatus,
	int stock,
	LocalDateTime createdAt,
	LocalDateTime soldAt,
	String thumbnail,
	String productName,
	List<SalesItemOptions> options
) {

}