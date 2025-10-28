package com.zeebra.global.ErrorCode;

import org.springframework.http.HttpStatus;

import com.zeebra.global.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ErrorCode {

	INVALID_ORDER_REQUEST("E400_INVALID_ORDER_REQUEST", HttpStatus.BAD_REQUEST, "유효하지 않은 주문 요청입니다"),
	INVALID_DELIVERY_ADDRESS("E400_INVALID_DELIVERY_ADDRESS", HttpStatus.BAD_REQUEST, "유효하지 않은 배송지 정보입니다"),

	ACCESS_DENIED("E403_ACCESS_DENIED", HttpStatus.FORBIDDEN,"접근 권한이 없습니다."),

	ORDER_NOT_FOUND("E404_ORDER_NOT_FOUND", HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다"),
	PRODUCT_NOT_FOUND("E404_PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND, "해당 상품을 찾을 수 없습니다."),

	ORDER_ALREADY_PROCESSED("E409_ORDER_ALREADY_PROCESSED", HttpStatus.CONFLICT, "이미 처리 중이거나 완료된 주문입니다"),
	ORDER_ALREADY_CONFIRMED("E409_ORDER_ALREADY_CONFIRMED", HttpStatus.CONFLICT, "이미 확정된 주문입니다"),
	ORDER_ALREADY_CANCELLED("E409_ORDER_ALREADY_CANCELLED", HttpStatus.CONFLICT, "이미 취소된 주문입니다"),
	ORDER_ALREADY_COMPLETED("E409_ORDER_ALREADY_COMPLETED", HttpStatus.CONFLICT, "이미 완료된 주문입니다"),

	PRODUCT_NOT_AVAILABLE("E422_PRODUCT_NOT_AVAILABLE", HttpStatus.UNPROCESSABLE_ENTITY, "주문할 수 없는 상품입니다"),
	PRODUCT_OUT_OF_STOCK("E422_PRODUCT_OUT_OF_STOCK", HttpStatus.UNPROCESSABLE_ENTITY, "상품 재고가 부족합니다"),
	ORDER_AMOUNT_MISMATCH("E422_ORDER_AMOUNT_MISMATCH", HttpStatus.UNPROCESSABLE_ENTITY, "주문 금액이 일치하지 않습니다"),
	ORDER_CANNOT_BE_CANCELLED("E422_ORDER_CANNOT_BE_CANCELLED", HttpStatus.UNPROCESSABLE_ENTITY, "취소할 수 없는 주문 상태입니다"),
	INVALID_COUPON("E422_INVALID_COUPON", HttpStatus.UNPROCESSABLE_ENTITY, "유효하지 않은 쿠폰입니다"),

	RATE_LIMIT_EXCEEDED("E429_RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, "주문 시도가 너무 많습니다. 잠시 후 다시 시도하세요.");

	private final String code;
	private final HttpStatus httpStatus;
	private final String message;
}