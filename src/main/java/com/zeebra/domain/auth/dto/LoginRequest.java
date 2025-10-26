package com.zeebra.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
	@NotBlank(message = "아이디를 입력하세요.") String identifier,
	@NotBlank(message = "비밀번호를 입력하세요.") String password) {
}