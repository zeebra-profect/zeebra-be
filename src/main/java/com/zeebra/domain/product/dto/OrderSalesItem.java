package com.zeebra.domain.product.dto;

import java.math.BigDecimal;
import java.util.List;

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

	public static OrderSalesItem of(Sales sales) {
		return OrderSalesItem.builder()
			.salesId(sales.getId())
			.productOptionId(sales.getProductOptionId())
			.quantity(sales.getStock())
			.price(sales.getPrice())
			.build();
	}

	public static List<OrderSalesItem> of(List<Sales> salesList) {
		return salesList.stream()
			.map(OrderSalesItem::of)
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