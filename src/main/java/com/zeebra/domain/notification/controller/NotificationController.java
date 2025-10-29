package com.zeebra.domain.notification.controller;

import com.zeebra.domain.auth.dto.MemberInfo;
import com.zeebra.domain.auth.service.AuthService;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.dto.NotificationsResponse;
import com.zeebra.domain.notification.service.NotificationService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.ErrorCode.AuthErrorCode;
import com.zeebra.global.exception.BusinessException;
import com.zeebra.global.security.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final AuthService authService;

    @GetMapping("/all")
    public ApiResponse<NotificationsResponse> getNotifications(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal) {

        MemberInfo member = authService.findById(principal.getMemberId());
        return ApiResponse.success(notificationService.getNotifications(member));
    }

    @GetMapping
    public ApiResponse<NotificationResponse> getNotification(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal) {
//        System.out.println("testestsetse");
        return null;
    }

    @GetMapping("/ws-token")
    public ApiResponse<String> getWebSocketToken(HttpServletRequest request) {
        System.out.println("asdfsadfasdfsadfsd: " + request.getSession().getId());
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("__Host-AT".equals(cookie.getName())) {
                    return ApiResponse.success(cookie.getValue());
                }
            }
        }
        throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
    }
}
