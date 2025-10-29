package com.zeebra.global.web;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class CookieUtil {

	private static final String ACCESS_TOKEN_COOKIE_NAME = "__Host-AT";
	private static final String REFRESH_TOKEN_COOKIE_NAME = "__Host-RT";

	private static final boolean HTTP_ONLY = true;
	private static final String PATH = "/";
	@Value("${auth.cookie.sameSite}")
	private static String SAME_SITE;
	@Value("${auth.cookie.secure}")
	private static boolean secure;

	private CookieUtil() {}

	public static String getCookieValue(HttpServletRequest request, String name) {
		if (request.getCookies() == null) { return null; }
		return Arrays.stream(request.getCookies())
			.filter(cookie -> cookie.getName().equals(name))
			.findFirst()
			.map(Cookie::getValue)
			.orElse(null);
	}

	public static void addAuthCookies(HttpServletResponse response,
		String accessToken,
		long accessTokenTtlMinutes,
		String refreshToken,
		long refreshTokenTtlDays) {

		ResponseCookie accessCookie = ResponseCookie
			.from(ACCESS_TOKEN_COOKIE_NAME, accessToken)
			.httpOnly(HTTP_ONLY).secure(secure).sameSite(SAME_SITE)
			.path(PATH)
			.maxAge(Duration.ofMinutes(accessTokenTtlMinutes))
			.build();

		ResponseCookie refreshCookie = ResponseCookie
			.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
			.httpOnly(HTTP_ONLY).secure(secure).sameSite(SAME_SITE)
			.path(PATH)
			.maxAge(Duration.ofDays(refreshTokenTtlDays))
			.build();

		response.addHeader("Set-Cookie", accessCookie.toString());
		response.addHeader("Set-Cookie", refreshCookie.toString());
	}

	public static void addAccessCookie(HttpServletResponse response,
		String accessToken,
		long accessTokenTtlMinutes) {

		ResponseCookie accessCookie = ResponseCookie
			.from(ACCESS_TOKEN_COOKIE_NAME, accessToken)
			.httpOnly(HTTP_ONLY).secure(secure).sameSite(SAME_SITE)
			.path(PATH)
			.maxAge(Duration.ofMinutes(accessTokenTtlMinutes))
			.build();

		response.addHeader("Set-Cookie", accessCookie.toString());
	}

	public static void clearAuthCookies(HttpServletResponse response) {
		ResponseCookie accessToken = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
			.httpOnly(HTTP_ONLY).secure(secure).sameSite(SAME_SITE)
			.path(PATH)
			.maxAge(0)
			.build();

		ResponseCookie refreshToken = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
			.httpOnly(HTTP_ONLY).secure(secure).sameSite(SAME_SITE)
			.path(PATH)
			.maxAge(0)
			.build();

		response.addHeader("Set-Cookie", refreshToken.toString());
		response.addHeader("Set-Cookie", accessToken.toString());
	}

	@Value("${cookie.secure:true}")
	public void setSecure(boolean secureValue) {
		secure = secureValue;
	}
}