package com.zeebra.domain.notification.controller;

import com.zeebra.domain.member.service.MemberService;
import com.zeebra.domain.notification.dto.NotificationRequest;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.dto.NotificationsResponse;
import com.zeebra.domain.notification.service.NotificationService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final MemberService memberService;

    // 알림 개별 조회. 실제 사용은 하지 않음
    @GetMapping("/{notificationId}")
    public ApiResponse<NotificationResponse> getNotificationByNotificationId(@PathVariable("notificationId") Long notificationId) {
        return ApiResponse.success(notificationService.getNotificationById(notificationId));
    }

    // 로그인 멤버 알림 전체 조회
    @GetMapping
    public ApiResponse<NotificationsResponse> getNotifications(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal) {
        return ApiResponse.success(notificationService.getNotifications(principal.getMemberId()));
    }

    @PutMapping("/{notificationId}")
    public ApiResponse<CompletableFuture<Void>> updateNotification(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal, @PathVariable("notificationId") Long notificationId) {
        return ApiResponse.success(notificationService.readNotification(principal.getMemberId(), notificationId));
    }

    @DeleteMapping("/{notificationId}")
    public ApiResponse<CompletableFuture<Void>> deleteNotification(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal, @PathVariable("notificationId") Long notificationId) {
        return ApiResponse.success(notificationService.deleteNotification(principal.getMemberId(), notificationId));
    }

    @PostMapping()
    public ApiResponse<NotificationResponse> createNotification(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal, NotificationRequest request) {
        request.setMemberId(principal.getMemberId());
        return ApiResponse.success(notificationService.createNotification(request));
    }
}