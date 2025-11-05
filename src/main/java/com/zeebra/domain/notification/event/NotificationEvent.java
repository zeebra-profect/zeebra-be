package com.zeebra.domain.notification.event;

import com.zeebra.domain.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public abstract class NotificationEvent {
    private Long memberId;
    private String nickname;
    private NotificationType notificationType;

    public abstract String generateUrl(Long objectId);

}
