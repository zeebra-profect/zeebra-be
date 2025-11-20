package com.zeebra.domain.webpush.service;

import com.zeebra.domain.common.InternalSupport;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.webpush.dto.WebPushRequest;
import com.zeebra.domain.webpush.entity.WebPush;
import com.zeebra.domain.webpush.repository.WebPushRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebPushService {
    private final WebPushRepository webPushRepository;
    private final InternalSupport internalSupport;
    private final Executor webPushWorkerExecutor;
    private final PushService pushService;

    public boolean isSubscribed(Long memberId) {
        Member member = internalSupport.findByMemberId(memberId);
        return !webPushRepository.findByMemberId(memberId).isEmpty();
    }

    @Async("mainWebPushExecutor")
    public CompletableFuture<Boolean> sendPush(Long memberId, String title, String body) {
        WebPush webPush = findByMemberId(memberId);

        if (pushService == null) {
            throw new NullPointerException("PushService 초기화 실패");
        }

        String content = String.format("{\"title\":\"%s\",\"body\":\"%s\"}",
                title,
                body);

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse response = send(pushService, webPush, content);
                int status = response.getStatusLine().getStatusCode();
                boolean success = (status == 201 || status == 200);
                if (success) {
                    log.debug("푸시 성공 - memberId: {}", memberId);
                } else {
                    log.warn("푸시 실패 - memberId: {}, status: {}", memberId, status);
                }
                return success;
            } catch (Exception e) {
                log.error("푸시 예외 - memberId: {}", memberId, e);
                return false;
            }
        }, webPushWorkerExecutor).thenApply(result -> result);
    }

    @Transactional
    public String saveSubscription(Long memberId, WebPushRequest request) {
        Member member = internalSupport.findByMemberId(memberId);
        webPushRepository.findByMemberIdAndDeviceInfo(memberId, request.getDeviceInfo())
                .ifPresent(existing -> {
                    log.info("기존 구독 정보 삭제 - id: {}", existing.getMemberId());
                    webPushRepository.delete(existing);
                });

        WebPush webPush = WebPush.builder()
                .memberId(memberId)
                .endpoint(request.getEndpoint())
                .p256dh(request.getP256dh())
                .auth(request.getAuth())
                .deviceInfo(request.getDeviceInfo())
                .build();
        webPushRepository.save(webPush);

        return "구독 성공";
    }

    @Transactional
    public String deleteSubscription(Long memberId) {
        Member member = internalSupport.findByMemberId(memberId);
        webPushRepository.deleteByMemberId(memberId);
        return "구독 해지 성공";
    }

    // 헬퍼 메서드

    public WebPush findByMemberId(Long memberId) {
        Member member = internalSupport.findByMemberId(memberId);
        return webPushRepository.findByMemberId(memberId).orElseThrow(() -> new NoSuchElementException("해당 멤버의 구독 정보가 없습니다."));
    }

    public HttpResponse send(PushService pushService, WebPush webPush, String content)
            throws Exception {
        var notification = new nl.martijndwars.webpush.Notification(
                webPush.getEndpoint(), webPush.getP256dh(), webPush.getAuth(),
                content.getBytes(StandardCharsets.UTF_8)
        );
        return pushService.send(notification);
    }
}

