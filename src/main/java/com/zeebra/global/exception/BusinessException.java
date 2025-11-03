package com.zeebra.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{
	private final ErrorCode errorCode;
	private final String message;

	public BusinessException(ErrorCode errorCode){
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.message = null;
	}

	public BusinessException(ErrorCode errorCode, String message){
		super(message);
		this.errorCode = errorCode;
		this.message = message;
	}

	@Override
	public String getMessage() {
		return message != null ? message : errorCode.getMessage();
	}
}