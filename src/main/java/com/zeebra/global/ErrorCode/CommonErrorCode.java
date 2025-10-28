package com.zeebra.global.ErrorCode;

import org.springframework.http.HttpStatus;

import com.zeebra.global.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {

	INVALID_REQUEST("E400_INVALID_REQUEST", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
	INVALID_PARAMETER("E400_INVALID_PARAMETER", HttpStatus.BAD_REQUEST, "Invalid request parameter"),
	INVALID_CLIENT_REQUEST_ID("E400_INVALID_CLIENT_REQUEST_ID", HttpStatus.BAD_REQUEST, "멱등성 키가 필요합니다"),

	AUTH_REQUIRED("E401_AUTH_REQUIRED", HttpStatus.UNAUTHORIZED, "Authentication required"),
	TOKEN_EXPIRED("E401_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "Access token has expired"),

	FORBIDDEN("E403_FORBIDDEN", HttpStatus.FORBIDDEN, "Access is forbidden"),
	NOT_FOUND("E404_NOT_FOUND", HttpStatus.NOT_FOUND, "Requested resource not found"),

	CONFLICT("E409_CONFLICT", HttpStatus.CONFLICT, "Conflict with current resource state"),
	DUPLICATE_CLIENT_REQUEST_ID("E409_DUPLICATE_CLIENT_REQUEST_ID", HttpStatus.CONFLICT, "중복된 요청입니다"),
	DUPLICATE_IDEMPOTENCY_KEY("E409_DUPLICATE_IDEMPOTENCY_KEY", HttpStatus.CONFLICT, "이미 처리된 요청입니다"),
	IDEMPOTENCY_CONFLICT("E409_IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT, "동일한 멱등성 키로 다른 내용의 요청이 이미 처리되었습니다"),

	UNSUPPORTED_MEDIA_TYPE("E415_UNSUPPORTED_MEDIA_TYPE", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type"),
	UNPROCESSABLE_ENTITY("E422_UNPROCESSABLE_ENTITY", HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violation"),
	INTERNAL_SERVER_ERROR("E500_INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

	private final String code;
	private final HttpStatus httpStatus;
	private final String message;
}