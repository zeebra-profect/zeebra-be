package com.zeebra.domain.notification.service;

import com.zeebra.domain.member.dto.MemberInfo;
import com.zeebra.domain.notification.dto.NotificationsResponse;
import com.zeebra.domain.notification.event.MemberLoginEvent;
import com.zeebra.domain.notification.event.MemberSignUpEvent;

public interface NotificationService {
    void handleMemberSignUp(MemberSignUpEvent memberInfo);

    NotificationsResponse getNotifications(MemberInfo member);

    NotificationsResponse sendNotifications(NotificationsResponse responses);

    void handleMemberLogin(MemberLoginEvent memberLoginEvent);

    void addNotification(Long memberId);
}