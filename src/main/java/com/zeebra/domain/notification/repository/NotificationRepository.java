package com.zeebra.domain.notification.repository;

import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Optional<Notification>> findByMemberIdOrderByCreatedTimeDesc(Long memberId);

    Optional<Notification> findByNotificationId(Long id);

    Optional<Notification> findByNotificationTypeAndMemberId(NotificationType notificationType, Long memberId);

    List<Optional<Notification>> findByMemberId(Long memberId);

}
