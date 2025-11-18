package com.zeebra.domain.notification.service;

import com.zeebra.domain.notification.dto.NotificationRequest;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.dto.NotificationsResponse;

import java.util.concurrent.CompletableFuture;

public interface NotificationService {

    CompletableFuture<NotificationResponse> createNotificationAsync(NotificationRequest notificationRequest);

    CompletableFuture<NotificationResponse> createNotificationAsyncPush(NotificationRequest notificationRequest);

    NotificationResponse getNotificationById(Long notificationId);

    NotificationsResponse getNotifications(Long memberId);

    CompletableFuture<Void> readNotification(Long memberId, Long notificationId);

    CompletableFuture<Void> deleteNotification(Long memberId, Long notificationId);

}