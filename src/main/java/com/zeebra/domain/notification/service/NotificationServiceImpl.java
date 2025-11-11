package com.zeebra.domain.notification.service;

import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.notification.component.NotificationUrlFactory;
import com.zeebra.domain.notification.dto.NotificationRequest;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.dto.NotificationsResponse;
import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.notification.entity.NotificationType;
import com.zeebra.domain.notification.event.NotificationEvent;
import com.zeebra.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final NotificationUrlFactory notificationUrlFactory;

    private boolean isValidNotificationType(NotificationType type) {
        return Arrays.asList(NotificationType.values()).contains(type);
    }

    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        Member member = memberRepository.findById(request.getMemberId()).orElseThrow(() -> new NoSuchElementException("해당하는 사용자가 없습니다."));

        if (request.getNotificationType() == null) {
            throw new IllegalArgumentException("타입 값이 없습니다.");
        }

        if (!isValidNotificationType(request.getNotificationType())) {
            throw new IllegalArgumentException("유효하지 않은 알림 타입입니다.");
        }

        String url = notificationUrlFactory.createUrl(request.getNotificationType(), request.getObject());
        Notification notification = new Notification(request.getMemberId(), request.getNotificationType(), url);

        try {
            notificationRepository.save(notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
        NotificationResponse.of(notification);

        return NotificationResponse.of(notification);
    }

    @EventListener
    @Async("notificationAsyncExecutor")
    public void NotificationEventListener(NotificationEvent event) {
        NotificationRequest request = new NotificationRequest();
        request.setMemberId(event.getMemberId());
        request.setNotificationType(event.getNotificationType());
        request.setObject(event.getObject());

        createNotificationAsync(request);
    }

    @Async("notificationAsyncExecutor")
    @Transactional
    public CompletableFuture<NotificationResponse> createNotificationAsync(NotificationRequest request) {
        System.out.println("Thread executing createNotificationAsync: " + Thread.currentThread().getName());
        return CompletableFuture.completedFuture(createNotification(request));
    }

    public NotificationResponse getNotificationById(Long notificationId) {
        Notification notification = notificationRepository.findByNotificationId(notificationId).orElseThrow(() -> new NoSuchElementException("해당하는 알림이 없습니다."));

        return NotificationResponse.of(notification);
    }

    public NotificationsResponse getNotifications(Long memberId) {

        if (memberId == null) {
            throw new NullPointerException("사용자의 id가 null입니다.");
        }

        Member member = memberRepository.findById(memberId).orElseThrow(() -> new NoSuchElementException("해당하는 사용자가 없습니다."));

        List<NotificationResponse> responses = new ArrayList<>();
        NotificationsResponse response = new NotificationsResponse(responses);
        for (Optional<Notification> notification : notificationRepository.findByMemberIdOrderByCreatedTimeDesc(memberId)) {
            NotificationResponse.of(notification.get());
            response.dtos().add(NotificationResponse.of(notification.get()));
        }

        if (responses.isEmpty()) {
            return null;
        }

        return response;
    }

    @Async("notificationAsyncExecutor")
    @Transactional
    public CompletableFuture<Void> readNotification(Long memberId, Long notificationId) {
        if (memberId == null) {
            throw new NullPointerException("사용자의 id가 null입니다.");
        }
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new NoSuchElementException("해당하는 사용자가 없습니다."));

        if (notificationId == null) {
            throw new NullPointerException("알림 id가 null입니다.");
        }

        Notification notification = notificationRepository.findByNotificationId(notificationId).orElseThrow(() -> new NoSuchElementException("해당하는 알림이 없습니다."));

        if (!Objects.equals(member.getId(), notification.getMemberId())) {
            throw new AccessDeniedException("권한이 없습니다.");
        }

        notification.read();
        notificationRepository.save(notification);

        return CompletableFuture.completedFuture(null);
    }

    @Async("notificationAsyncExecutor")
    @Transactional
    public CompletableFuture<Void> deleteNotification(Long memberId, Long notificationId) {
        if (memberId == null) {
            throw new NullPointerException("사용자의 id가 null입니다.");
        }
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new NoSuchElementException("해당하는 사용자가 없습니다."));

        if (notificationId == null) {
            throw new NullPointerException("알림 id가 null입니다.");
        }

        Notification notification = notificationRepository.findByNotificationId(notificationId).orElseThrow(() -> new NoSuchElementException("해당하는 알림이 없습니다."));

        if (!Objects.equals(member.getId(), notification.getMemberId())) {
            throw new AccessDeniedException("권한이 없습니다.");
        }

        notificationRepository.delete(notification);

        return CompletableFuture.completedFuture(null);
    }

}