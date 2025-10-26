package com.zeebra.domain.auth.controller;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeebra.domain.auth.dto.LoginRequest;
import com.zeebra.domain.auth.dto.LoginResponse;
import com.zeebra.domain.auth.dto.LoginSuccess;
import com.zeebra.domain.auth.service.AuthService;
import com.zeebra.global.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth API", description = "인증 관련 API")
public class AuthController {
	private final AuthService authService;

	@Operation(
		summary = "회원 로그인",
		description = "아이디와 비밀번호를 이용하여 로그인합니다."
	)
	@PostMapping("/login")
	@Transactional
	public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {

		LoginSuccess result = authService.login(request);

		ResponseCookie AccessToken = ResponseCookie
			.from("__Host-AT", result.accessToken())
			.httpOnly(true).secure(true).sameSite("Lax")
			.path("/")
			.maxAge(Duration.ofMinutes(result.accessTokenTtlMinutes()))
			.build();

		ResponseCookie RefreshToken = ResponseCookie
			.from("__Host-RT", result.refreshToken())
			.httpOnly(true).secure(true).sameSite("Lax")
			.path("/")
			.maxAge(Duration.ofDays(result.refreshTokenTtlDays()))
			.build();

		response.addHeader("Set-Cookie", AccessToken.toString());
		response.addHeader("Set-Cookie", RefreshToken.toString());

		return ApiResponse.success(LoginResponse.of(result.memberInfo()));
	}
}