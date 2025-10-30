package com.zeebra.domain.notification.service;

import com.zeebra.domain.member.dto.MemberInfo;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.dto.NotificationsResponse;
import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.notification.entity.NotificationType;
import com.zeebra.domain.notification.event.MemberLoginEvent;
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
    private final NotificationHandler notificationHandler;

    @EventListener
    @Transactional
    public void handleMemberSignUp(MemberSignUpEvent member) {
        System.out.println("handleMemberSignUp 들어옴");
        Notification notification = Notification.builder()
                .memberId(member.getMemberId())
                .notificationType(member.getNotificationType())
                .build();

        notificationRepository.save(notification);
        NotificationResponse notificationResponse = new NotificationResponse(notification.getNotificationType(), false, notification.getNotificationType().getNoticeText(), notification.getCreatedTime());
        List<NotificationResponse> notificationResponses = new ArrayList<>();
        notificationResponses.add(notificationResponse);
    }


    public NotificationsResponse getNotifications(MemberInfo member) {

        List<Notification> notifications = notificationRepository.findByMemberIdOrderByCreatedTimeDesc(member.memberId());
        NotificationsResponse responses = new NotificationsResponse(new ArrayList<>());
        for (Notification notification : notifications) {
            responses.dtos().add(NotificationResponse.of(notification));
        }
        notificationHandler.sendNotifications(member.memberId(), responses.dtos());
        return responses;
    }

    public NotificationsResponse sendNotifications(NotificationsResponse responses) {

        return responses;
    }

    @EventListener
    public void handleMemberLogin(MemberLoginEvent memberLoginEvent) {
        System.out.println("handleMemberLogin 실행!");
        Notification notification = Notification.builder()
                .memberId(memberLoginEvent.getMemberId())
                .notificationType(NotificationType.ORDER_CONFIRMED)
                .build();
        NotificationResponse loginEventNotification = new NotificationResponse(notification.getNotificationType(), false, notification.getNotificationType().getNoticeText(), notification.getCreatedTime());
        notificationRepository.save(notification);
        notificationHandler.sendNotification(notification.getMemberId(), loginEventNotification);

    }


    public void addNotification(Long memberId) {
        Notification notification = Notification.builder()
                .memberId(memberId)
                .notificationType(NotificationType.NEW_MESSAGE)
                .build();

        System.out.println("notification test: " + notification);
        notificationRepository.save(notification);
        NotificationResponse loginEventNotification = new NotificationResponse(notification.getNotificationType(), false, notification.getNotificationType().getNoticeText(), notification.getCreatedTime());
        notificationHandler.sendNotification(notification.getMemberId(), loginEventNotification);
    }

}