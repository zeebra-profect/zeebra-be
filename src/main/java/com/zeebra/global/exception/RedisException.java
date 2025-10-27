package com.zeebra.global.exception;

import com.zeebra.global.ErrorCode.RedisErrorCode;

import lombok.Getter;

@Getter
public class RedisException extends RuntimeException{
	private final RedisErrorCode errorCode;

	public RedisException(RedisErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}