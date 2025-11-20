package com.zeebra.domain.notification.event;

import com.zeebra.domain.notification.dto.NotificationRequest;
import com.zeebra.domain.notification.entity.NotificationType;
import com.zeebra.domain.notification.service.NotificationService;
import com.zeebra.domain.webpush.service.WebPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

    @Lazy
    private final NotificationService notificationService;
    @Lazy
    private final WebPushService webPushService;

    @EventListener
    @Async("mainNotificationExecutor")
    public void onNotificationEvent(NotificationEvent event) {
        log.info("Processing notification event for member: {}", event.getMemberId());
        NotificationType notificationType = event.getNotificationType();
        notificationType.createMessage(event.getDisplayText());
        NotificationRequest request = NotificationRequest.builder()
                .memberId(event.getMemberId())
                .notificationType(notificationType)
                .imgUrl(event.getImgUrl())
                .build();
        try {
            notificationService.createNotificationAsyncPush(event, request);
        } catch (Exception e) {
            log.error("Notification creation failed", e);
        }
    }

    @EventListener
    @Async("mainWebPushExecutor")
    public void onWebPushEvent(WebPushEvent event) {
        log.info("Processing web push event for member: {}", event.getMemberId());
        String body = event.getNotificationType().createMessage(event.getDisplayText());
        try {
            webPushService.sendPush(event.getMemberId(), event.getNotificationType().getNotificationTitle(), body);
            log.info("Web push event content: {}", body);
        } catch (Exception e) {
            log.error("Notification creation failed", e);
        }
    }
}
