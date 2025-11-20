package com.zeebra.domain.order.dto;

import com.zeebra.domain.order.entity.OrderType;
import com.zeebra.global.ErrorCode.OrderErrorCode;
import com.zeebra.global.exception.BusinessException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record CreateOrderRequest(
	@NotBlank String clientRequestId,
	Long cartId,
	Long productOptionId,
	@Valid SalesItem salesItem
) {
	public CreateOrderRequest(String clientRequestId, long id, OrderType orderType) {
		this(clientRequestId, orderType == OrderType.CART ? id: null, orderType == OrderType.DIRECT ? id: null, null);
	}

	public CreateOrderRequest(String clientRequestId, SalesItem salesItem){
		this(clientRequestId, null, null, salesItem);
	}

	public void validate() {
		if (cartId == null && productOptionId == null && salesItem == null) {
			log.error("[주문 생성 실패] cartId와 productOptionId, salesItem이 모두 null입니다. clientRequestId: {}",
				clientRequestId);
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
		}

		if ((cartId != null && salesItem != null) || (cartId != null && productOptionId != null) || (salesItem != null && productOptionId != null)) {
			log.error("[주문 생성 실패] cartId와 productOptionId, salesItem을 동시에 사용할 수 없습니다. clientRequestId: {}",
				clientRequestId);
			throw new BusinessException(OrderErrorCode.INVALID_ORDER_REQUEST);
		}
	}
}