package com.zeebra.domain.notification.service;

import com.zeebra.domain.notification.dto.NotificationsResponse;
import com.zeebra.domain.notification.event.MemberSignUpEvent;

public interface NotificationService {
    void handleMemberSignUp(MemberSignUpEvent memberInfo);

    NotificationsResponse getNotifications(Long memberId);
}
