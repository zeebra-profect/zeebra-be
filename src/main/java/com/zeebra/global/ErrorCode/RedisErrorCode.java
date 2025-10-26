package com.zeebra.global.ErrorCode;

import org.springframework.http.HttpStatus;

import com.zeebra.global.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RedisErrorCode implements ErrorCode {
	REDIS_CONNECTION_ERROR("E500_REDIS_CONNECTION_ERROR", HttpStatus.SERVICE_UNAVAILABLE, "Redis 연서버에 연결할 수 없습니다."),
	REDIS_OPERATION_ERROR("E500_REDIS_OPERATION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Redis 작업 중 오류가 발생했습니다");

	private final String code;
	private final HttpStatus httpStatus;
	private final String message;
}