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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final MemberService memberService;

    @GetMapping("/all")
    public ApiResponse<NotificationsResponse> getNotifications(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal) {
        return ApiResponse.success(notificationService.getNotifications(principal.getMemberId()));
    }

    @PostMapping()
    public ApiResponse<NotificationResponse> createNotification(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal, NotificationRequest request) {
        request.setMemberId(principal.getMemberId());
        return ApiResponse.success(notificationService.createNotification(request));
    }
}