package com.zeebra.global.ErrorCode;

import org.springframework.http.HttpStatus;

import com.zeebra.global.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemberErrorCode implements ErrorCode {

	INVALID_REQUEST("E400_INVALID_REQUEST", HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
	INVALID_EMAIL("E400_INVALID_EMAIL", HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
	INVALID_PASSWORD("E400_INVALID_PASSWORD", HttpStatus.BAD_REQUEST, "비밀번호가 정책에 맞지 않습니다."),
	PASSWORD_MISMATCH("E400_PASSWORD_MISMATCH", HttpStatus.BAD_REQUEST, "비밀번호와 확인 비밀번호가 일치하지 않습니다."),
	INVALID_GENDER("E400_INVALID_GENDER", HttpStatus.BAD_REQUEST, "성별 정보가 올바르지 않습니다."),
	INVALID_BIRTH("E400_INVALID_BIRTH", HttpStatus.BAD_REQUEST, "생년월일이 유효하지 않습니다."),

	MEMBER_NOT_FOUND("E404_MEMBER_NOT_FOUND",HttpStatus.NOT_FOUND,"존재하지 않는 회원입니다."),

	DUPLICATE_LOGIN_ID("E409_DUPLICATE_LOGIN_ID", HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
	DUPLICATE_EMAIL("E409_DUPLICATE_EMAIL", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
	DUPLICATE_NICKNAME("E409_DUPLICATE_NICKNAME", HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

	EMAIL_SEND_FAILED("E500_EMAIL_SEND_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다."),
	DATABASE_ERROR("E500_DATABASE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "데이터베이스 처리 중 오류가 발생했습니다."),
	UNKNOWN_ERROR("E500_UNKNOWN_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "예기치 못한 오류가 발생했습니다.");


	private final String code;
	private final HttpStatus httpStatus;
	private final String message;
}