package com.zeebra.domain.notification.controller;

import com.zeebra.domain.notification.dto.NotificationRequest;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.dto.NotificationsResponse;
import com.zeebra.domain.notification.service.NotificationService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService notificationService;

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
    public CompletableFuture<ApiResponse<Object>> updateNotification(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal, @PathVariable("notificationId") Long notificationId) {
        return notificationService.readNotification(principal.getMemberId(), notificationId)
                .thenApply(v -> {
                    // 비동기 작업 완료 후 실행
                    log.info("알림 {} 읽음 처리 완료", notificationId);
                    return ApiResponse.successMessage(notificationId.toString());
                })
                .exceptionally(ex -> {
                    // 에러 처리
                    log.error("알림 읽음 처리 실패", ex);
                    return ApiResponse.errorMessage(ex.getMessage());
                });
    }

    @DeleteMapping("/{notificationId}")
    public CompletableFuture<ApiResponse<NotificationsResponse>> deleteNotification(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal, @PathVariable("notificationId") Long notificationId) {
        return notificationService.deleteNotification(principal.getMemberId(), notificationId)
                .thenApply(v -> ApiResponse.success(
                        notificationService.getNotifications(principal.getMemberId())
                ));
    }

    @PostMapping
    public CompletableFuture<ApiResponse<NotificationsResponse>> createNotification(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal, @RequestBody NotificationRequest request) {
        request.setMemberId(principal.getMemberId());

        return notificationService.createNotificationAsync(request)
                .thenApply(v -> ApiResponse.success(
                        notificationService.getNotifications(principal.getMemberId())
                ));
    }
}