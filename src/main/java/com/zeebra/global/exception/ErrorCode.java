package com.zeebra.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
	HttpStatus getHttpStatus();
	String getCode();
	String getMessage();
}