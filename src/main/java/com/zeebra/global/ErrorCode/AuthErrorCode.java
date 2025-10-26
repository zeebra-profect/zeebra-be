package com.zeebra.global.ErrorCode;

import org.springframework.http.HttpStatus;

import com.zeebra.global.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {

	INVALID_REQUEST("E400_INVALID_REQUEST", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
	INVALID_EMAIL("E400_INVALID_EMAIL", HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
	INVALID_PASSWORD("E400_INVALID_PASSWORD", HttpStatus.BAD_REQUEST, "비밀번호가 정책에 맞지 않습니다."),
	PASSWORD_MISMATCH("E400_PASSWORD_MISMATCH", HttpStatus.BAD_REQUEST, "비밀번호와 확인 비밀번호가 일치하지 않습니다."),
	INVALID_GENDER("E400_INVALID_GENDER", HttpStatus.BAD_REQUEST, "성별 정보가 올바르지 않습니다."),
	INVALID_BIRTH("E400_INVALID_BIRTH", HttpStatus.BAD_REQUEST, "생년월일이 유효하지 않습니다."),
	MISSING_IDENTIFIER("E400_MISSING_IDENTIFIER", HttpStatus.BAD_REQUEST, "아이디가 비어 있습니다."),
	MISSING_PASSWORD("E400_MISSING_PASSWORD", HttpStatus.BAD_REQUEST, "비밀번호가 비어 있습니다."),
	INVALID_IDENTIFIER_FORMAT("E400_INVALID_IDENTIFIER_FORMAT", HttpStatus.BAD_REQUEST, "아이디 형식이 올바르지 않습니다."),

	INVALID_CREDENTIALS("E401_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
	AUTH_REQUIRED("E401_AUTH_REQUIRED", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
	AUTH_COOKIE_MISSING("E401_AUTH_COOKIE_MISSING", HttpStatus.UNAUTHORIZED, "인증 쿠키가 없습니다."),
	TOKEN_EXPIRED("E401_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "액세스 토큰이 만료되었습니다."),
	TOKEN_MALFORMED("E401_TOKEN_MALFORMED", HttpStatus.UNAUTHORIZED, "토큰 형식이 올바르지 않습니다."),
	TOKEN_SIGNATURE_INVALID("E401_TOKEN_SIGNATURE_INVALID", HttpStatus.UNAUTHORIZED, "토큰 서명이 유효하지 않습니다."),
	TOKEN_TYPE_INVALID("E401_TOKEN_TYPE_INVALID", HttpStatus.UNAUTHORIZED, "잘못된 토큰 유형입니다."), // AT가 필요한 곳에 RT 사용 등
	REFRESH_TOKEN_EXPIRED("E401_REFRESH_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),
	REFRESH_TOKEN_INVALID("E401_REFRESH_TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."),

	ACCOUNT_DISABLED("E403_ACCOUNT_DISABLED", HttpStatus.FORBIDDEN, "비활성화된 계정입니다."),
	ACCOUNT_DELETED_SOFT("E403_ACCOUNT_DELETED_SOFT", HttpStatus.FORBIDDEN, "삭제 예정(소프트 삭제) 상태의 계정입니다."),
	CSRF_TOKEN_MISSING("E403_CSRF_TOKEN_MISSING", HttpStatus.FORBIDDEN, "CSRF 토큰이 포함되지 않았습니다."),
	CSRF_TOKEN_INVALID("E403_CSRF_TOKEN_INVALID", HttpStatus.FORBIDDEN, "CSRF 토큰이 유효하지 않습니다."),

	DUPLICATE_LOGIN_ID("E409_DUPLICATE_LOGIN_ID", HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
	DUPLICATE_EMAIL("E409_DUPLICATE_EMAIL", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
	DUPLICATE_NICKNAME("E409_DUPLICATE_NICKNAME", HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
	SOFT_DELETED_EMAIL_CONFLICT("E409_SOFT_DELETED_EMAIL_CONFLICT", HttpStatus.CONFLICT, "삭제된 계정에서 사용 중인 이메일입니다."),
	SOFT_DELETED_NICKNAME_CONFLICT("E409_SOFT_DELETED_NICKNAME_CONFLICT", HttpStatus.CONFLICT, "삭제된 계정에서 사용 중인 닉네임입니다."),

	UNSUPPORTED_MEDIA_TYPE("E415_UNSUPPORTED_MEDIA_TYPE", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type은 application/json 이어야 합니다."),

	RATE_LIMIT_EXCEEDED("E429_RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 너무 많습니다. 잠시 후 다시 시도하세요."),


	DATABASE_ERROR("E500_DATABASE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 처리 중 오류가 발생했습니다."),
	UNKNOWN_ERROR("E500_UNKNOWN_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "예기치 못한 오류가 발생했습니다.");


	private final String code;
	private final HttpStatus httpStatus;
	private final String message;
}