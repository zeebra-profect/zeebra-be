package com.zeebra.domain.notification.dto;

import java.time.LocalDateTime;

import com.zeebra.domain.member.dto.MemberInfo;
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
    private Long notificationId;
    private MemberInfo memberId;
    private NotificationType notificationType;
    private LocalDateTime createdAt;
    private boolean isRead;
}