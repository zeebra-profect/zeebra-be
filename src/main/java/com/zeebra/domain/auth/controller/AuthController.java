package com.zeebra.domain.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeebra.domain.auth.dto.LoginRequest;
import com.zeebra.domain.auth.dto.LoginResponse;
import com.zeebra.domain.auth.dto.LoginSuccess;
import com.zeebra.domain.auth.service.AuthService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.web.CookieUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth API", description = "인증 관련 API")
public class AuthController {
	private static final String ACCESS_TOKEN_COOKIE_NAME = "__Host-AT";
	private static final String REFRESH_TOKEN_COOKIE_NAME = "__Host-RT";


	private final AuthService authService;

	@Operation(
		summary = "회원 로그인",
		description = "아이디와 비밀번호를 이용하여 로그인합니다."
	)
	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {

		LoginSuccess result = authService.login(request);

		CookieUtil.addAuthCookies(response, result.accessToken(), result.accessTokenTtlMinutes(), result.refreshToken(), result.refreshTokenTtlDays());

		return ApiResponse.success(LoginResponse.of(result.memberInfo()));
	}

	@Operation(
		summary = "로그아웃",
		description = "로그아웃을 합니다."
	)
	@PostMapping("/logout")
	public ApiResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {

		String accessToken = CookieUtil.getCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);
		String refreshToken = CookieUtil.getCookieValue(request, REFRESH_TOKEN_COOKIE_NAME);

		authService.logout(accessToken, refreshToken);

		CookieUtil.clearAuthCookies(response);

		return ApiResponse.success("Logged out");
	}
}