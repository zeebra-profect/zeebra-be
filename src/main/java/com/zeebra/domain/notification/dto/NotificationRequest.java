package com.zeebra.domain.notification.dto;

import com.zeebra.domain.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private Long memberId;
    private NotificationType notificationType;
    private Object object;
    private String imgUrl;
}