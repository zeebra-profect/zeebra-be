package com.zeebra.domain.notification.dto;

import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        Long memberId,
        NotificationType notificationType,
        boolean isRead,
        String noticeText,
        LocalDateTime createdTime,
        String url,
        String imgUrl
) {

    public static NotificationResponse of(Notification notification) {
        return new NotificationResponse(notification.getNotificationId(), notification.getMemberId(), notification.getNotificationType(), notification.isRead(), notification.getNotificationType().getNoticeBasicText(), notification.getCreatedTime(), notification.getUrl(), notification.getImgUrl());
    }
}
