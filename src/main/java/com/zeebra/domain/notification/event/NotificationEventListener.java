package com.zeebra.domain.notification.event;

import com.zeebra.domain.notification.dto.NotificationRequest;
import com.zeebra.domain.notification.entity.NotificationType;
import com.zeebra.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @EventListener
    @Async("notificationAsyncExecutor")
    public void onApplicationEvent(NotificationEvent event) {
        log.info("Processing notification event for member: {}", event.getMemberId());
        NotificationType notificationType = event.getNotificationType();
        notificationType.createMessage(event.getDisplayText());
        NotificationRequest request = NotificationRequest.builder()
                .memberId(event.getMemberId())
                .notificationType(notificationType)
                .object(event.getObject())
                .imgUrl(event.getImgUrl())
                .build();

        try {
            notificationService.createNotificationAsync(request);
        } catch (Exception e) {
            log.error("Notification creation failed", e);
        }
    }
}
