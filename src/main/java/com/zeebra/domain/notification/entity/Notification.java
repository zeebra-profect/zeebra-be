package com.zeebra.domain.notification.entity;

import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;
    private Long memberId;
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;
    private boolean isRead;

    @Builder
    public Notification(NotificationType notificationType, Long memberId)
    {
        this.memberId = memberId;
        this.notificationType = notificationType;
        this.isRead = false;
    }
}
