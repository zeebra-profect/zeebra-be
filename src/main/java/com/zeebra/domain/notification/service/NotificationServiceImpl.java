package com.zeebra.domain.notification.service;

import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.notification.event.MemberSignUpEvent;
import com.zeebra.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{
    private final NotificationRepository notificationRepository;

    @EventListener
    @Transactional
    public void handleMemberSignUp(MemberSignUpEvent member)
    {
        System.out.println("handleMemberSignUp 들어옴");
        Notification notification = Notification.builder()
                .memberId(member.getMemberId())
                        .notificationType(member.getNotificationType())
                                .build();

        notificationRepository.save(notification);
        System.out.println("notification:" + notification);
    }
}
