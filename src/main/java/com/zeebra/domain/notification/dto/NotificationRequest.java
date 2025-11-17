package com.zeebra.domain.notification.dto;

import com.zeebra.domain.notification.entity.NotificationType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private Long memberId;
    private NotificationType notificationType;
    private Object object;
    private String imgUrl;
}