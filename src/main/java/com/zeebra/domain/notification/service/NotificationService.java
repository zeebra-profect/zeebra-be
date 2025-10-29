package com.zeebra.domain.notification.service;

import com.zeebra.domain.member.dto.MemberInfo;
import com.zeebra.domain.notification.dto.NotificationsResponse;
import com.zeebra.domain.notification.event.MemberSignUpEvent;

public interface NotificationService {
    void handleMemberSignUp(MemberSignUpEvent memberInfo);

    public NotificationsResponse getNotifications(MemberInfo member);

    void getNotification(Object event);

    NotificationsResponse sendNotifications(NotificationsResponse responses);

//    NotificationResponse sendNotification();
}