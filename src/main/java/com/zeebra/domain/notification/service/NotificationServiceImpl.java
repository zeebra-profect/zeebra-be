package com.zeebra.domain.notification.service;

import com.zeebra.domain.common.InternalSupport;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.notification.component.NotificationUrlFactory;
import com.zeebra.domain.notification.dto.NotificationRequest;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.dto.NotificationsResponse;
import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.notification.entity.NotificationType;
import com.zeebra.domain.notification.event.NotificationEvent;
import com.zeebra.domain.notification.event.WebPushEvent;
import com.zeebra.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationUrlFactory notificationUrlFactory;
    private final InternalSupport internalSupport;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Executor notificationWorkerExecutor;

    private boolean isValidNotificationType(NotificationType type) {
        return Arrays.asList(NotificationType.values()).contains(type);
    }

    @Async("mainNotificationExecutor")
    public CompletableFuture<NotificationResponse> createNotificationAsync(NotificationRequest request) {
        if (request.getNotificationType() == null) {
            throw new IllegalArgumentException("타입 값이 없습니다.");
        }

        if (!isValidNotificationType(request.getNotificationType())) {
            throw new IllegalArgumentException("유효하지 않은 알림 타입입니다.");
        }

        String url = notificationUrlFactory.createUrl(request.getNotificationType(), request.getObject());
        Member member = internalSupport.findByMemberId(request.getMemberId());
        Notification notification = new Notification(member.getId(), request.getNotificationType(), url, request.getImgUrl());

        return CompletableFuture.supplyAsync(() -> internalSupport.saveNotification(notification), notificationWorkerExecutor)
                .thenApply(NotificationResponse::of);
    }

    @Async("mainNotificationExecutor")
    public CompletableFuture<NotificationResponse> createNotificationAsyncPush(NotificationEvent event, NotificationRequest request) {
        if (request.getNotificationType() == null) {
            throw new IllegalArgumentException("타입 값이 없습니다.");
        }

        if (!isValidNotificationType(request.getNotificationType())) {
            throw new IllegalArgumentException("유효하지 않은 알림 타입입니다.");
        }

        String url = notificationUrlFactory.createUrl(request.getNotificationType(), request.getObject());
        Member member = internalSupport.findByMemberId(request.getMemberId());
        Notification notification = new Notification(member.getId(), request.getNotificationType(), url, request.getImgUrl());

        return CompletableFuture.supplyAsync(() -> {
                    Notification savedNotification = internalSupport.saveNotification(notification);
                    applicationEventPublisher.publishEvent(new WebPushEvent(
                            member.getId(),
                            event.getDisplayText(),
                            savedNotification.getNotificationType()
                    ));
                    return savedNotification;
                }, notificationWorkerExecutor)
                .thenApply(NotificationResponse::of);

    }


    public NotificationResponse getNotificationById(Long notificationId) {
        Notification notification = notificationRepository.findByNotificationId(notificationId).orElseThrow(() -> new NoSuchElementException("해당하는 알림이 없습니다."));

        return NotificationResponse.of(notification);
    }

    public NotificationsResponse getNotifications(Long memberId) {
        Member member = internalSupport.findByMemberId(memberId);
        List<NotificationResponse> responses = new ArrayList<>();
        NotificationsResponse response = new NotificationsResponse(responses);
        for (Optional<Notification> notification : notificationRepository.findByMemberIdOrderByCreatedTimeDesc(member.getId())) {
            NotificationResponse.of(notification.get());
            response.dtos().add(NotificationResponse.of(notification.get()));
        }

        if (responses.isEmpty()) {
            return null;
        }

        return response;
    }

    @Async("mainNotificationExecutor")
    public CompletableFuture<Void> readNotification(Long memberId, Long notificationId) {
        Member member = internalSupport.findByMemberId(memberId);
        Notification notification = internalSupport.findByNotificationId(notificationId);

        if (!Objects.equals(member.getId(), notification.getMemberId())) {
            throw new AccessDeniedException("권한이 없습니다.");
        }

        notification.read();

        return CompletableFuture.supplyAsync(() -> internalSupport.saveNotification(notification), notificationWorkerExecutor)
                .thenApply(saved -> null); // Void 반환
    }

    @Async("mainNotificationExecutor")
    public CompletableFuture<Void> deleteNotification(Long memberId, Long notificationId) {
        Member member = internalSupport.findByMemberId(memberId);
        Notification notification = internalSupport.findByNotificationId(notificationId);

        if (!Objects.equals(member.getId(), notification.getMemberId())) {
            throw new AccessDeniedException("권한이 없습니다.");
        }

        return CompletableFuture.runAsync(() -> internalSupport.deleteNotification(notification), notificationWorkerExecutor);
    }

}