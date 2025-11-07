package com.zeebra.domain.notification.service;

import com.zeebra.domain.notification.dto.NotificationRequest;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.dto.NotificationsResponse;

public interface NotificationService {

    NotificationResponse createNotification(NotificationRequest notificationRequest);

    NotificationResponse getNotificationById(Long notificationId);

    NotificationsResponse getNotifications(Long memberId);

//    void handleMemberSignUp(NotiSignUpEvent memberInfo);
//    void handleMemberLogin(NotiLoginEvent notiLoginEvent);
}