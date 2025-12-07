package com.zeebra.domain.product.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.zeebra.domain.product.entity.Sales;
import com.zeebra.global.ErrorCode.OrderErrorCode;
import com.zeebra.global.exception.BusinessException;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record OrderSalesItem(
	Long salesId,
	Long productOptionId,
	int quantity,
	BigDecimal price
) {
	@Builder
	public OrderSalesItem(Long salesId,Long productOptionId, int quantity, BigDecimal price){
		this.salesId = salesId;
		this.productOptionId = productOptionId;
		this.quantity = quantity;
		this.price = price;
	}

	public static OrderSalesItem of(Sales sales, int quantity) {
		return OrderSalesItem.builder()
			.salesId(sales.getId())
			.productOptionId(sales.getProductOptionId())
			.quantity(quantity)
			.price(sales.getPrice())
			.build();
	}

	public static List<OrderSalesItem> of(Map<Sales, Integer> salesQuantityMap) {
		if (salesQuantityMap == null || salesQuantityMap.isEmpty()) {
			return List.of();
		}
		return salesQuantityMap.entrySet().stream()
			.map(entry -> of(entry.getKey(), entry.getValue()))
			.toList();
	}

	public BigDecimal lineAmount() {
		return price.multiply(BigDecimal.valueOf(quantity));
	}

	public void validateSalesItem() {
		if (this.quantity() <= 0) {
			log.error("[주문 생성 실패] 주문 수량이 0 이하입니다. saleId: {}, quantity: {}",
				this.salesId(), this.quantity());
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
		}

		if (this.price().compareTo(BigDecimal.ZERO) <= 0) {
			log.error("[주문 생성 실패] 상품 가격이 0 이하입니다. saleId: {}, price: {}",
				this.salesId(), this.price());
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
		}
	}
}