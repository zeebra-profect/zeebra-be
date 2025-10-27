package com.zeebra.global.ErrorCode;

import org.springframework.http.HttpStatus;

import com.zeebra.global.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {

	INVALID_REQUEST("E400_INVALID_REQUEST", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
	INVALID_INPUT("E400_INVALID_INPUT", HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
	INVALID_EMAIL("E400_INVALID_EMAIL", HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
	INVALID_PASSWORD("E400_INVALID_PASSWORD", HttpStatus.BAD_REQUEST, "비밀번호가 정책에 맞지 않습니다."),
	PASSWORD_MISMATCH("E400_PASSWORD_MISMATCH", HttpStatus.BAD_REQUEST, "비밀번호와 확인 비밀번호가 일치하지 않습니다."),
	INVALID_GENDER("E400_INVALID_GENDER", HttpStatus.BAD_REQUEST, "성별 정보가 올바르지 않습니다."),
	INVALID_BIRTH("E400_INVALID_BIRTH", HttpStatus.BAD_REQUEST, "생년월일이 유효하지 않습니다."),
	MISSING_IDENTIFIER("E400_MISSING_IDENTIFIER", HttpStatus.BAD_REQUEST, "아이디가 비어 있습니다."),
	MISSING_PASSWORD("E400_MISSING_PASSWORD", HttpStatus.BAD_REQUEST, "비밀번호가 비어 있습니다."),
	INVALID_IDENTIFIER_FORMAT("E400_INVALID_IDENTIFIER_FORMAT", HttpStatus.BAD_REQUEST, "아이디 형식이 올바르지 않습니다."),

	INVALID_CREDENTIALS("E401_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
	AUTH_REQUIRED("E401_AUTH_REQUIRED", HttpStatus.UNAUTHORIZED, "인증이 필요합니다. 로그인해주세요."),
	TOKEN_EXPIRED("E401_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "액세스 토큰이 만료되었습니다."),
	TOKEN_INVALID("E401_TOKEN_INVALID", HttpStatus.UNAUTHORIZED,
		"인증 정보가 유효하지 않습니다. 다시 로그인해주세요."),


	ACCESS_DENIED("E403_ACCESS_DENIED", HttpStatus.FORBIDDEN,"접근 권한이 없습니다."),

	DUPLICATE_LOGIN_ID("E409_DUPLICATE_LOGIN_ID", HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
	DUPLICATE_EMAIL("E409_DUPLICATE_EMAIL", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
	DUPLICATE_NICKNAME("E409_DUPLICATE_NICKNAME", HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

	RATE_LIMIT_EXCEEDED("E429_RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 너무 많습니다. 잠시 후 다시 시도하세요."),

	DATABASE_ERROR("E500_DATABASE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 처리 중 오류가 발생했습니다."),
	UNKNOWN_ERROR("E500_UNKNOWN_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "예기치 못한 오류가 발생했습니다.");


	private final String code;
	private final HttpStatus httpStatus;
	private final String message;
}