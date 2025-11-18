package com.zeebra.domain.webpush.service;

import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.webpush.dto.WebPushRequest;
import com.zeebra.domain.webpush.entity.WebPush;
import com.zeebra.domain.webpush.repository.WebPushRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushService {
    private final WebPushRepository webPushRepository;
    private final MemberRepository memberRepository;

    @Value("${vapid.public.key}")
    private String vapidPublicKey;

    @Value("${vapid.private.key}")
    private String vapidPrivateKey;

    @Value("${vapid.subject}")
    private String vapidSubject;

    public boolean isSubscribed(Long memberId) {

        if (memberId == null) {
            throw new NullPointerException("사용자의 id가 null입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("해당하는 사용자가 없습니다."));

        if (member.getId() != memberId) {
            throw new AccessDeniedException("권한이 없습니다.");
        }

        return !webPushRepository.findByMemberId(memberId).isEmpty();
    }

    @Async
    public void sendPush(Long memberId, Notification notification) {
        if (memberId == null) {
            throw new NullPointerException("사용자의 id가 null입니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("해당하는 사용자가 없습니다."));

        if (member.getId() != memberId) {
            throw new AccessDeniedException("권한이 없습니다.");
        }

        List<WebPush> webPushList = webPushRepository.findByMemberId(memberId);
        if (webPushList.isEmpty()) {
            log.info("해당 멤버의 구독 정보가 없습니다.");
            return;
        }

        PushService pushService = initializePushService();

        if (pushService == null) {
            return;
        }

        String payload = createPushPayload(notification);

        int successCount = 0;
        int failCount = 0;

        for (WebPush webPush : webPushList) {
            try {
                nl.martijndwars.webpush.Notification pushNotification =
                        new nl.martijndwars.webpush.Notification(
                                webPush.getEndpoint(),
                                webPush.getP256dh(),
                                webPush.getAuth(),
                                payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)
                        );
//                System.out.println("payload string: '" + payload + "'");
//                System.out.println("payload length: " + payload.length());
//                System.out.println("VAPID PUBLIC KEY = " + vapidPublicKey);
//                System.out.println("VAPID PRIVATE KEY = " + vapidPrivateKey);
                org.apache.http.HttpResponse response = pushService.send(pushNotification);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 201) {
                    successCount++;
                    log.debug("푸시 발송 성공 - webPushId: {}", webPush.getWebPushId());
                } else if (statusCode == 410) {
                    // 구독 만료 - 삭제
                    webPushRepository.delete(webPush);
                    log.info("만료된 구독 삭제 - webPushId: {}", webPush.getWebPushId());
                    failCount++;
                } else {
                    log.warn("푸시 발송 실패 - webPushId: {}, statusCode: {}",
                            webPush.getWebPushId(), statusCode);
                    failCount++;
                }

            } catch (Exception e) {
                failCount++;
                log.error("푸시 발송 중 예외 - webPushId: {}, error: {}",
                        webPush.getWebPushId(), e.getMessage());
            }
        }

        log.info("푸시 발송 완료 - memberId: {}, 성공: {}, 실패: {}",
                memberId, successCount, failCount);

    }

    @Transactional
    public String saveSubscription(Long memberId, WebPushRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("해당하는 사용자가 없습니다."));

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
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new NoSuchElementException("해당하는 사용자가 없습니다."));
        webPushRepository.deleteByMemberId(memberId);
        return "구독 해지 성공";
    }

    private PushService initializePushService() {
        try {
            PushService pushService = new PushService();
            pushService.setPublicKey(vapidPublicKey);
            pushService.setPrivateKey(vapidPrivateKey);
            pushService.setSubject(vapidSubject);
            return pushService;
        } catch (GeneralSecurityException e) {
            log.error("푸시 서비스 초기화 실패 (VAPID 키 오류): {}", e.getMessage());
            return null;
        }
    }

    private String createPushPayload(Notification notification) {
        return String.format("{\"title\":\"%s\",\"body\":\"%s\"}",
                notification.getNotificationType().getNoticeBasicText(),
                notification.getNotificationType().getNoticeBasicText());
    }


}
