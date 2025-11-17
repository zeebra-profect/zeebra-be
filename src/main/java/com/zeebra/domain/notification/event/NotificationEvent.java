package com.zeebra.domain.notification.event;

import com.zeebra.domain.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent {
    private Long memberId;
    private String nickname;
    private NotificationType notificationType;
    private Object object;
    private String imgUrl;

    public NotificationEvent(Long memberId, NotificationType type, Object object, String imgUrl) {
        this.memberId = memberId;
        this.notificationType = type;
        this.object = object;
        this.imgUrl = imgUrl;
    }

}

