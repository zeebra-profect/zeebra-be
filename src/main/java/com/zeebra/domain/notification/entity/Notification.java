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
    private String url;

    @Builder
    public Notification(Long memberId, NotificationType notificationType) {
        this.memberId = memberId;
        this.notificationType = notificationType;
        this.isRead = false;
    }

    public void CreateUrl() {
        switch (this.notificationType) {
            case NotificationType.TEST:
                this.url = "/dummy/testId";
                break;
            case NotificationType.ORDER_CONFIRMED:
                this.url = "/mypage/orderhistory";
                break;
            case NotificationType.ORDER_SHIPPED:
                this.url = "/mypage/orderhistory";
                break;
            case NotificationType.ORDER_DELIVERED:
                this.url = "/mypage/orderhistory";
                break;
            default:
                this.url = null;
                break;
        }
    }


}
