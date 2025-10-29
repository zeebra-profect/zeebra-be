package com.zeebra.domain.notification.dto;

import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        NotificationType notificationType,
        boolean isRead,
        String noticeText,
        LocalDateTime createdTime
) {

    public static NotificationResponse of(Notification notification) {
        return new NotificationResponse(notification.getNotificationType(), notification.isRead(), notification.getNotificationType().getNoticeText(), notification.getCreatedTime());
    }
}
