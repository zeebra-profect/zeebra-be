package com.zeebra.global.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.zeebra.global.ApiResponse;
import com.zeebra.global.ErrorCode.CommonErrorCode;
import com.zeebra.global.ErrorCode.RedisErrorCode;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@Hidden
public class GlobalExceptionHandler {

	@Value("${spring.profiles.active:prod}")
	private String activeProfile;

	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
	public ResponseEntity<ApiResponse<ErrorData>> handleValidation(BindException ex) {
		log.warn("[파라미터 오류] {}", ex.getMessage());
		
		CommonErrorCode errorCode = CommonErrorCode.INVALID_PARAMETER;

		if (isDevelopment()) {
			log.debug("Error details - code: {}, message: {}",
				errorCode.getCode(),
				errorCode.getMessage(),
				ex);
		}

		List<ErrorDetail> errorDetails = ex.getBindingResult().getFieldErrors().stream()
			.map(error -> new ErrorDetail(
				error.getField(),
				error.getDefaultMessage()
			))
			.collect(Collectors.toList());
		
		ErrorData errorData = new ErrorData(errorCode.getCode(), errorDetails);
		
		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(errorData, errorCode.getMessage()));
	}

	@ExceptionHandler(AuthorizationDeniedException.class)
	public ResponseEntity<ApiResponse<ErrorData>> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
		log.warn("[권한 거부] {}", ex.getMessage());
		
		CommonErrorCode errorCode = CommonErrorCode.FORBIDDEN;

		if (isDevelopment()) {
			log.debug("Error details - code: {}, message: {}",
				errorCode.getCode(),
				errorCode.getMessage(),
				ex);
		}

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

		if (isDevelopment()) {
			log.debug("Error details - code: {}, message: {}",
				errorCode.getCode(),
				errorCode.getMessage(),
				ex);
		}

		ErrorData errorData = new ErrorData(errorCode.getCode(), ex.getMessage());

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(errorData, errorCode.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<ErrorData>> handleAllException(Exception ex) {
		log.error("[알 수 없는 에러] 발생: 메시지 = {}", ex.getMessage(), ex);
		
		CommonErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;

		if (isDevelopment()) {
			log.debug("Error details - code: {}, message: {}",
				errorCode.getCode(),
				errorCode.getMessage(),
				ex);
		}

		ErrorData errorData = new ErrorData(errorCode.getCode(), ex.getMessage());

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(errorData, errorCode.getMessage()));
	}

	@ExceptionHandler(RedisException.class)
	public ResponseEntity<ApiResponse<ErrorData>> handleRedisConnectionException(RedisException ex) {
		log.error("Redis error: {}", ex.getMessage(), ex);

		RedisErrorCode errorCode = ex.getErrorCode();
		ErrorData errorData = new ErrorData(errorCode.getCode(), ex.getMessage());

		return ResponseEntity
			.status(errorCode.getHttpStatus())
			.body(ApiResponse.error(errorData, errorCode.getMessage()));
	}

	private boolean isDevelopment() {
		return "dev".equals(activeProfile) || "local".equals(activeProfile);
	}
}