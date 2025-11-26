package com.zeebra.domain.notification.controller;

import com.zeebra.domain.common.InternalSupport;
import com.zeebra.domain.notification.dto.NotificationRequest;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.dto.NotificationsResponse;
import com.zeebra.domain.notification.entity.NotificationType;
import com.zeebra.domain.notification.event.NotificationEvent;
import com.zeebra.domain.notification.service.NotificationService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final InternalSupport internalSupport;

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

    @GetMapping("/all")
    public ResponseEntity<Map<Long, Long>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    @PutMapping("/{notificationId}")
    public CompletableFuture<ApiResponse<Object>> updateNotification(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal, @PathVariable("notificationId") Long notificationId) {
        Long memberId = principal.getMemberId();
        return notificationService.readNotification(memberId, notificationId)
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
        Long memberId = principal.getMemberId();
        return notificationService.deleteNotification(memberId, notificationId)
                .thenApply(v -> ApiResponse.success(
                        notificationService.getNotifications(principal.getMemberId())
                ));
    }

    @PostMapping
    public ApiResponse<NotificationResponse> createNotification(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal, @RequestBody NotificationRequest request) {
        NotificationEvent event = new NotificationEvent(principal.getMemberId(), NotificationType.TEST, principal.getUserLoginId(), null);
        return ApiResponse.success(notificationService.createNotificationAsyncPush(event, request).join());
    }


//    @PostMapping("/sync")
//    public ApiResponse<NotificationResponse> createNotificationSync(@RequestBody NotificationRequest request) {
//        return ApiResponse.success(notificationService.createNotificationSync(request));
//    }

    @PostMapping("/sync")
    public ResponseEntity<NotificationResponse> createNotificationSync(@RequestBody NotificationRequest request) {
        return ResponseEntity.ok(notificationService.createNotificationSync(request));
    }

    @PutMapping("/{notificationId}/sync")
    public ResponseEntity<Void> updateNotificationSync(@RequestHeader("memberId") Long memberId,
                                                       @PathVariable("notificationId") Long notificationId) {
        notificationService.readNotificationSync(memberId, notificationId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{notificationId}/sync")
    public ResponseEntity<Void> deleteNotificationSync(@RequestParam Long memberId, @PathVariable("notificationId") Long notificationId) {
        notificationService.deleteNotification(memberId, notificationId);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/async")
    public CompletableFuture<ResponseEntity<NotificationResponse>> createNotificationAsync(@RequestBody NotificationRequest request) {
        return CompletableFuture.completedFuture(ResponseEntity.ok(notificationService.createNotificationAsync(request).join()));
    }

    @PostMapping("/broadcast/sync")
    public void broadcast(@RequestBody NotificationRequest request) {
        notificationService.broadcast(request);
    }

    @PostMapping("/broadcast/prepare")
    public void prepare() {
        internalSupport.createTestMembers(10000);
    }


}