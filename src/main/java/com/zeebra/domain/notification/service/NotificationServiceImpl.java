package com.zeebra.domain.notification.service;

import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.notification.dto.NotificationRequest;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.dto.NotificationsResponse;
import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    // 로그인 시 내 모든 알림 조회하는 메서드. 필수 기능
    public NotificationsResponse getNotifications(Long memberId) {
        List<Notification> notifications = notificationRepository.findByMemberIdOrderByCreatedTimeDesc(memberId);
        NotificationsResponse responses = new NotificationsResponse(new ArrayList<>());
        for (Notification notification : notifications) {
            responses.dtos().add(NotificationResponse.of(notification));
        }
        return responses;
    }

    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        Member member = memberRepository.findById(request.getMemberId()).orElseThrow(
                () -> new NoSuchElementException("해당하는 사용자가 없습니다."));

        if (request.getNotificationType() == null) {
            throw new IllegalArgumentException("유효한 Type이 아니거나 null입니다.");
        }

        Notification notification = new Notification(request.getMemberId(), request.getNotificationType());
        notification.CreateUrl();

        try {
            notificationRepository.save(notification);
        } catch (Exception e) {

        }
        NotificationResponse.of(notification);
        return NotificationResponse.of(notification);
    }

//    @EventListener
//    @Transactional
//    public void handleMemberSignUp(NotiSignUpEvent member) {
//        System.out.println("handleMemberSignUp 들어옴");
//        Notification notification = Notification.builder()
//                .memberId(member.getMemberId())
//                .notificationType(member.getNotificationType())
//                .build();
//
//        notificationRepository.save(notification);
//        NotificationResponse notificationResponse = new NotificationResponse(notification.getNotificationType(), false, notification.getNotificationType().getNoticeText(), notification.getCreatedTime(),);
//        List<NotificationResponse> notificationResponses = new ArrayList<>();
//        notificationResponses.add(notificationResponse);
//    }


//    @EventListener
//    public void handleMemberLogin(NotiLoginEvent notiLoginEvent) {
//        System.out.println("handleMemberLogin 실행!");
//        Notification notification = Notification.builder()
//                .memberId(notiLoginEvent.getMemberId())
//                .notificationType(NotificationType.ORDER_CONFIRMED)
//                .build();
//        NotificationResponse loginEventNotification = new NotificationResponse(notification.getNotificationType(), false, notification.getNotificationType().getNoticeText(), notification.getCreatedTime());
//        notificationRepository.save(notification);
//
//    }

}