package com.zeebra.domain.notification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberLoginEvent {
    private Long memberId;
    private String nickname;
}
