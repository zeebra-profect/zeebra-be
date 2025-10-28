package com.zeebra.domain.notification.event;

import com.zeebra.domain.notification.entity.NotificationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberSignUpEvent {
    private Long memberId;
    private String nickname;
    private NotificationType notificationType;

    public MemberSignUpEvent(Long memberId, String nickname) {
        this.memberId = memberId;
        this.nickname = nickname;
        this.notificationType = NotificationType.SIGN_UP;
    }

}
