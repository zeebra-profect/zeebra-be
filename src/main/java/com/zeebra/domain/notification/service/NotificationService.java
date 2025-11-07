package com.zeebra.domain.notification.service;

import com.zeebra.domain.notification.dto.NotificationRequest;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.dto.NotificationsResponse;

import java.util.concurrent.CompletableFuture;

public interface NotificationService {

    NotificationResponse createNotification(NotificationRequest notificationRequest);

    CompletableFuture<NotificationResponse> createNotificationAsync(NotificationRequest notificationRequest);

    NotificationResponse getNotificationById(Long notificationId);

    NotificationsResponse getNotifications(Long memberId);

}