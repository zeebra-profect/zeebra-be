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

    @GetMapping("/status")
    public ApiResponse<Boolean> isSubscribed(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal) {
        return ApiResponse.success(webPushService.isSubscribed(principal.getMemberId()));
    }

    @PostMapping
    public ApiResponse<String> subscribe(
            @AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal,
            @RequestBody WebPushRequest request) {
        webPushService.saveSubscription(principal.getMemberId(), request);
        return ApiResponse.success(webPushService.saveSubscription(principal.getMemberId(), request));
    }

    @DeleteMapping
    public ApiResponse<String> unsubscribe(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal) {
        System.out.println("들어오니?");

        ApiResponse<String> result = ApiResponse.success(webPushService.deleteSubscription(principal.getMemberId()));
        System.out.println(result.getData());
        return result;
//        return ApiResponse.success(webPushService.deleteSubscription(principal.getMemberId()));
    }

}
