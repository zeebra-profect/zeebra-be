package com.zeebra.global.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.zeebra.global.ApiResponse;
import com.zeebra.global.ErrorCode.CommonErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
	public ResponseEntity<ApiResponse<ErrorData>> handleValidation(BindException ex) {
		log.warn("[파라미터 오류] {}", ex.getMessage());
		
		CommonErrorCode errorCode = CommonErrorCode.INVALID_PARAMETER;

		List<ErrorDetail> errorDetails = ex.getBindingResult().getFieldErrors().stream()
			.map(error -> new ErrorDetail(
				error.getField(),
				error.getDefaultMessage()
			))
			.collect(Collectors.toList());
		
		// ErrorData: code + details(List<ErrorDetail>)
		ErrorData errorData = new ErrorData(errorCode.getCode(), errorDetails);
		
		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(errorData, errorCode.getMessage()));
	}

	@ExceptionHandler(AuthorizationDeniedException.class)
	public ResponseEntity<ApiResponse<ErrorData>> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
		log.warn("[권한 거부] {}", ex.getMessage());
		
		CommonErrorCode errorCode = CommonErrorCode.FORBIDDEN;
		
		// ErrorData: code + details(String)
		ErrorData errorData = new ErrorData(errorCode.getCode(), "접근 권한이 없습니다.");
		
		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(errorData, errorCode.getMessage()));
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<ErrorData>> handleBusinessException(BusinessException ex) {
		ErrorCode errorCode = ex.getErrorCode();

		log.error("[비즈니스 예외] 발생: 코드 = {}, 메시지 = {}",
			errorCode.getCode(), errorCode.getMessage());
		
		ErrorData errorData = new ErrorData(errorCode.getCode(), ex.getMessage());

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(errorData, errorCode.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<ErrorData>> handleAllException(Exception ex) {
		log.error("[알 수 없는 에러] 발생: 메시지 = {}", ex.getMessage(), ex);
		
		CommonErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;

		ErrorData errorData = new ErrorData(errorCode.getCode(), ex.getMessage());

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(errorData, errorCode.getMessage()));
	}
}