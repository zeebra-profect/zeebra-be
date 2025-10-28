package com.zeebra.domain.notification.dto;

import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.notification.entity.NotificationType;

public record NotificationResponse(
        NotificationType notificationType,
        boolean isRead
) {
    public static NotificationResponse of(Notification notification) {
        return new NotificationResponse(notification.getNotificationType(), notification.isRead());
    }
}
