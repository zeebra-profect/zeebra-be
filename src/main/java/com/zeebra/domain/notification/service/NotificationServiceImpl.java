package com.zeebra.domain.notification.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zeebra.domain.member.dto.MemberInfo;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.dto.NotificationsResponse;
import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.notification.event.MemberSignUpEvent;
import com.zeebra.domain.notification.handler.NotificationHandler;
import com.zeebra.domain.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

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
        notificationHandler.sendNotification(member.getMemberId(), notificationResponses);
    }


    public NotificationsResponse getNotifications(MemberInfo member) {

        List<Notification> notifications = notificationRepository.findByMemberIdOrderByCreatedTimeDesc(member.memberId());
        NotificationsResponse responses = new NotificationsResponse(new ArrayList<>());
        for (Notification notification : notifications) {
            responses.dtos().add(NotificationResponse.of(notification));
        }
        notificationHandler.sendNotification(member.memberId(), responses.dtos());
        return responses;
    }

    public NotificationsResponse sendNotifications(NotificationsResponse responses) {

        return responses;
    }

    @EventListener
    public void getNotification(Object event) {
//        System.out.println("event : " + event);
//        System.out.println("여기까지 오는지???");
    }
}