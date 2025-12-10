package com.zeebra.global.ErrorCode;

import org.springframework.http.HttpStatus;

import com.zeebra.global.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SalesErrorCode implements ErrorCode {

	INVALID_SALES_REQUEST("E400_INVALID_SALES_REQUEST", HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다"),
	INVALID_STATUS_TRANSITION("E400_INVALID_STATUS_TRANSITION", HttpStatus.BAD_REQUEST,"유효하지 않은 상태 전이입니다."),
	INVALID_QUANTITY("E400_INVALID_QUANTITY", HttpStatus.BAD_REQUEST, "유효하지 않은 수량입니다."),

	ACCESS_DENIED("E403_ACCESS_DENIED", HttpStatus.FORBIDDEN,"접근 권한이 없습니다."),

	SALES_NOT_FOUND("E404_SALES_NOT_FOUND", HttpStatus.NOT_FOUND, "판매 정보를 찾을 수 없습니다"),
	PRODUCT_NOT_FOUND("E404_PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND, "상품 정보를 찾을 수 없습니다"),

	ALREADY_IN_PROGRESS("E409_ALREADY_IN_PROGRESS", HttpStatus.CONFLICT, "이미 처리 중이거나 처리가 완료 되었습니다."),

	SALES_NOT_AVAILABLE("E422_SALES_NOT_AVAILABLE", HttpStatus.UNPROCESSABLE_ENTITY, "판매 가능한 상품이 아닙니다."),
	OUT_OF_STOCK("E422_OUT_OF_STOCK", HttpStatus.UNPROCESSABLE_ENTITY, "재고가 부족합니다"),
	SALES_AMOUNT_MISMATCH("E422_SALES_AMOUNT_MISMATCH", HttpStatus.UNPROCESSABLE_ENTITY, "금액이 일치하지 않습니다"),
	CANNOT_BE_CANCELLED("E422_CANNOT_BE_CANCELLED", HttpStatus.UNPROCESSABLE_ENTITY, "취소할 수 없는 상태입니다"),
	RATE_LIMIT_EXCEEDED("E429_RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, "시도가 너무 많습니다. 잠시 후 다시 시도하세요.");


	private final String code;
	private final HttpStatus httpStatus;
	private final String message;
}