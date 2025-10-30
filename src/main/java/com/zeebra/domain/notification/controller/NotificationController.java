package com.zeebra.domain.notification.controller;

import com.zeebra.domain.member.dto.MemberInfo;
import com.zeebra.domain.member.service.MemberService;
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
        MemberInfo member = memberService.findById(principal.getMemberId());
        return ApiResponse.success(notificationService.getNotifications(member));
    }

    @PostMapping
    public void addNotification(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal) {
        notificationService.addNotification(principal.getMemberId());
    }

    @GetMapping
    public ApiResponse<NotificationResponse> getNotification(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal) {
        return null;
    }
}