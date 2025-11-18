package com.zeebra.domain.webpush.controller;

import com.zeebra.domain.webpush.dto.WebPushRequest;
import com.zeebra.domain.webpush.service.WebPushService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class WebPushController {

    private final WebPushService webPushService;

    @PostMapping
    public ApiResponse<?> subscribe(
            @AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal,
            @RequestBody WebPushRequest request) {
        webPushService.saveSubscription(principal.getMemberId(), request);
        return ApiResponse.success(webPushService.saveSubscription(principal.getMemberId(), request));
    }

    @DeleteMapping
    public ApiResponse<?> unsubscribe(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal) {
        return ApiResponse.success(webPushService.deleteSubscription(principal.getMemberId()));
    }

}
