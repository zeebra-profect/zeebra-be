package com.zeebra.global.security.jwt;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.global.ErrorCode.AuthErrorCode;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
	private static final String AT_COOKIE = "__Host-AT";
	private static final String RT_COOKIE = "__Host-RT";

	private final JwtProvider jwtProvider;
	private final MemberRepository memberRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws
		ServletException, IOException {
		String accessToken = readCookie(request, AT_COOKIE);

		try {
			if (accessToken != null) {
				if (jwtProvider.isValid(accessToken) && jwtProvider.isAccessToken(accessToken)) {
					Authentication authentication = jwtProvider.toAuthentication(accessToken);
					SecurityContextHolder.getContext().setAuthentication(authentication);
					filterChain.doFilter(request, response);
					return;
				}


				if (jwtProvider.isExpired(accessToken)) {
					boolean refreshed = tryRefreshUsingRefreshToken(request, response);
					if (refreshed) {
						filterChain.doFilter(request, response);
						return;
					} else {
						setAuthError(request, AuthErrorCode.TOKEN_EXPIRED);
					}
				} else {
					setAuthError(request, AuthErrorCode.TOKEN_MALFORMED);
				}

			} else {
				boolean refreshed = tryRefreshUsingRefreshToken(request, response);
				if (!refreshed) {
					setAuthError(request, AuthErrorCode.AUTH_COOKIE_MISSING);
				}
			}
		} catch (ExpiredJwtException e) {
			setAuthError(request, AuthErrorCode.TOKEN_EXPIRED);
		} catch (SignatureException e) {
			setAuthError(request, AuthErrorCode.TOKEN_SIGNATURE_INVALID);
		} catch (JwtException e) {
			setAuthError(request, AuthErrorCode.TOKEN_MALFORMED);
		}

		filterChain.doFilter(request, response);
	}

	private boolean tryRefreshUsingRefreshToken(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = readCookie(request, RT_COOKIE);
		if (refreshToken == null) {
			setAuthError(request, AuthErrorCode.AUTH_COOKIE_MISSING);
			return false;
		}

		if (!jwtProvider.isValid(refreshToken)) {
			setAuthError(request, AuthErrorCode.REFRESH_TOKEN_INVALID);
			return false;
		}
		if (!jwtProvider.isRefreshToken(refreshToken)) {
			setAuthError(request, AuthErrorCode.TOKEN_TYPE_INVALID);
			return false;
		}

		Long memberId = jwtProvider.getSubjectAsLong(refreshToken); // JwtProvider에 헬퍼 추가
		if (memberId == null) {
			setAuthError(request, AuthErrorCode.REFRESH_TOKEN_INVALID);
			return false;
		}

		Optional<Member> optional = memberRepository.findByIdAndDeletedAtIsNull(memberId);
		if (optional.isEmpty()){
			setAuthError(request, AuthErrorCode.INVALID_CREDENTIALS);
			return false;
		}

		Member member = optional.get();
		String userLoginId = member.getUserLoginId();
		String role = member.getRole().toString();

		String newAccessToken = jwtProvider.createAccessToken(memberId, userLoginId, role, 60L);

		ResponseCookie AccessToken = ResponseCookie.from(AT_COOKIE, newAccessToken)
			.httpOnly(true).secure(true).sameSite("Lax").path("/")
			.maxAge(Duration.ofMinutes(60))
			.build();
		response.addHeader("Set-Cookie", AccessToken.toString());

		Authentication authentication = jwtProvider.toAuthentication(newAccessToken);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		return true;
	}

	private void setAuthError(HttpServletRequest request, AuthErrorCode code) {
		request.setAttribute("AUTH_ERROR_CODE", code);
	}

	private String readCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(name)) {
				return cookie.getValue();
			}
		}
		return null;
	}
}