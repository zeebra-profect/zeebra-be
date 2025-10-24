package com.zeebra.global.ErrorCode;

import org.springframework.http.HttpStatus;

import com.zeebra.global.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {

	// 공통 error code
	INVALID_PARAMETER("E400_INVALID_PARAMETER", HttpStatus.BAD_REQUEST, "Invalid request parameter"),
	AUTH_REQUIRED("E401_AUTH_REQUIRED", HttpStatus.UNAUTHORIZED, "Authentication required"),
	TOKEN_EXPIRED("E401_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "Access token has expired"),
	FORBIDDEN("E403_FORBIDDEN", HttpStatus.FORBIDDEN, "Access is forbidden"),
	NOT_FOUND("E404_NOT_FOUND", HttpStatus.NOT_FOUND, "Requested resource not found"),
	CONFLICT("E409_CONFLICT", HttpStatus.CONFLICT, "Conflict with current resource state"),
	UNSUPPORTED_MEDIA_TYPE("E415_UNSUPPORTED_MEDIA_TYPE", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type"),
	UNPROCESSABLE_ENTITY("E422_UNPROCESSABLE_ENTITY", HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violation"),
	INTERNAL_SERVER_ERROR("E500_INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

	// 도메인별 error code, 필요에 따라서 추가하여 사용해주세요.

	private final String code;
	private final HttpStatus httpStatus;
	private final String message;
}