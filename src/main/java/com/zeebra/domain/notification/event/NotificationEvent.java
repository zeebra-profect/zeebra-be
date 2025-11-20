package com.zeebra.domain.notification.event;

import com.zeebra.domain.notification.entity.NotificationType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class NotificationEvent extends ApplicationEvent {
    private Long memberId;
    private NotificationType notificationType;
    private String displayText;
    private String imgUrl;

    // 기본 이벤트
    public NotificationEvent(Long memberId, NotificationType type, String displayText, String imgUrl) {
        super(NotificationEvent.class);
        this.memberId = memberId;
        this.displayText = displayText;
        this.notificationType = type;
        this.imgUrl = imgUrl;
    }

}

