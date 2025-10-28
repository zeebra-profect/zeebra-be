package com.zeebra.domain.notification.repository;

import com.zeebra.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByMemberIdOrderByCreatedTimeDesc(Long memberId);
}
