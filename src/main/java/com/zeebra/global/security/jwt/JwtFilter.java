package com.zeebra.global.security.jwt;

import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.global.ErrorCode.AuthErrorCode;
import com.zeebra.global.web.CookieUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login",
            "/api/auth/signup",
            "/swagger-ui",
            "/v3/api-docs"
    };
    private static final String ACCESS_TOKEN_COOKIE_NAME = "__Host-AT";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "__Host-RT";
    private static final long ACCESS_TOKEN_EXPIRATION_MINUTES = 60L;

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;
    // private final RedisService redisService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws
            ServletException, IOException {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        if (isPublicPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (requestPath.startsWith("/api/products") && "GET".equals(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String accessToken = CookieUtil.getCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);

            if (accessToken != null && processAccessToken(accessToken)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (tryRefreshToken(request, response)) {
                filterChain.doFilter(request, response);
                return;
            }

        } catch (ExpiredJwtException e) {
            setAuthError(request, AuthErrorCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            setAuthError(request, AuthErrorCode.TOKEN_INVALID);
        } catch (Exception e) {
            setAuthError(request, AuthErrorCode.TOKEN_INVALID);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String requestPath) {
        for (String publicPath : PUBLIC_PATHS) {
            if (requestPath.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean processAccessToken(String accessToken) {
        // if (redisService.isBlacklisted(accessToken)) {
        //     return false;
        // }

        if (!jwtProvider.isValid(accessToken)) {
            return false;
        }

        if (!jwtProvider.isAccessToken(accessToken)) {
            return false;
        }

        try {
            Authentication authentication = jwtProvider.toAuthentication(accessToken);
            if (authentication == null) {
                return false;
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private boolean tryRefreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = CookieUtil.getCookieValue(request, REFRESH_TOKEN_COOKIE_NAME);

        if (refreshToken == null) {
            setAuthError(request, AuthErrorCode.TOKEN_INVALID);
            return false;
        }

        // if (redisService.isBlacklisted(refreshToken)) {
        //     setAuthError(request, AuthErrorCode.TOKEN_INVALID);
        //     return false;
        // }

        if (!jwtProvider.isValid(refreshToken)) {
            setAuthError(request, AuthErrorCode.TOKEN_EXPIRED);
            return false;
        }

        if (!jwtProvider.isRefreshToken(refreshToken)) {
            setAuthError(request, AuthErrorCode.TOKEN_INVALID);
            return false;
        }

        Long memberId = jwtProvider.getSubjectAsLong(refreshToken);
        if (memberId == null) {
            setAuthError(request, AuthErrorCode.TOKEN_INVALID);
            return false;
        }

        Optional<Member> optional = memberRepository.findByIdAndDeletedAtIsNull(memberId);
        if (optional.isEmpty()) {
            setAuthError(request, AuthErrorCode.INVALID_CREDENTIALS);
            return false;
        }

        Member member = optional.get();

        String newAccessToken = jwtProvider.createAccessToken(
                member.getId(),
                member.getUserLoginId(),
                member.getRole().toString(),
                ACCESS_TOKEN_EXPIRATION_MINUTES
        );


        CookieUtil.addAccessCookie(response, newAccessToken, ACCESS_TOKEN_EXPIRATION_MINUTES);

        // Authentication 설정
        try {
            Authentication authentication = jwtProvider.toAuthentication(newAccessToken);
            if (authentication == null) {
                setAuthError(request, AuthErrorCode.TOKEN_INVALID);
                return false;
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);
            return true;

        } catch (Exception e) {
            setAuthError(request, AuthErrorCode.TOKEN_INVALID);
            return false;
        }
    }
    
    private void setAuthError(HttpServletRequest request, AuthErrorCode code) {
        request.setAttribute("AUTH_ERROR_CODE", code);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
//        String path = request.getRequestURI();
//        return path.startsWith("/ws")
//                || path.startsWith("/api/auth");  // 로그인/회원가입은 인증 불필요
        return false;
    }

}