package com.zeebra.domain.notification.service;

import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.dto.NotificationsResponse;
import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.notification.event.MemberSignUpEvent;
import com.zeebra.domain.notification.handler.NotificationHandler;
import com.zeebra.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private NotificationHandler notificationHandler;

    @EventListener
    @Transactional
    public void handleMemberSignUp(MemberSignUpEvent member) {
        System.out.println("handleMemberSignUp 들어옴");
        Notification notification = Notification.builder()
                .memberId(member.getMemberId())
                .notificationType(member.getNotificationType())
                .build();

        notificationRepository.save(notification);
        NotificationResponse notificationResponse = new NotificationResponse(notification.getNotificationType(), false);
        notificationHandler.sendNotification(member.getMemberId(), notificationResponse);
    }

    public NotificationsResponse getNotifications(Long memberId) {
        List<Notification> notifications = notificationRepository.findByMemberIdOrderByCreatedTimeDesc(memberId);


        NotificationsResponse responses = new NotificationsResponse(new ArrayList<>());
        for (Notification notification : notifications) {
            responses.dtos().add(NotificationResponse.of(notification));
        }

        return responses;
    }
}
