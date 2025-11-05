package com.zeebra.domain.notification.event;

import com.zeebra.domain.notification.entity.NotificationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotiSignUpEvent extends NotificationEvent {

    public NotiSignUpEvent(Long memberId, String nickname, NotificationType notificationType) {
        super(memberId, nickname, notificationType);
    }

    @Override
    public String generateUrl(Long objectId) {
        return null;
    }
}
