package com.zeebra.domain.notification.event;

import com.zeebra.domain.notification.entity.NotificationType;
import lombok.Getter;

@Getter
public class NotiLoginEvent extends NotificationEvent {

    public NotiLoginEvent(Long memberId, String nickname, NotificationType notificationType) {
        super(memberId, nickname, notificationType);
    }

    @Override
    public String generateUrl(Long objectId) {
        return null;
    }
}
