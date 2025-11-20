package com.zeebra.domain.order.dto;

import java.math.BigDecimal;

import com.zeebra.global.ErrorCode.OrderErrorCode;
import com.zeebra.global.exception.BusinessException;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record SalesItem(
	@NotNull(message = "거래 정보는 필수값입니다.") Long tradeId,
	@NotNull(message = "상품 정보는 필수값입니다.") Long salesId,
	@Positive(message = "상품은 최소 1개 이상 주문해야 합니다.") int quantity,
	@DecimalMin(value = "0.0", message = "상품 가격은 0원 이상이어야 합니다.") BigDecimal price) {
	public static SalesItem of(Long tradeId, Long salesId, int count, BigDecimal price) {
		return new SalesItem(tradeId, salesId, count, price);
	}

	public static SalesItem of(Long salesId, int stock, BigDecimal price) {
		return new SalesItem(null, salesId, stock, price);
	}

	public void validateSalesItem() {
		if (this.quantity() <= 0) {
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST, "주문 수량이 0 이하입니다.");
		}

		if (this.price().compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST, "상품 가격이 0 미만입니다.");
		}

	}
}