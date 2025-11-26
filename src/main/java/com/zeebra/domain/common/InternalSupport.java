package com.zeebra.domain.common;

import com.zeebra.domain.member.entity.Gender;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.entity.Role;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Component
public class InternalSupport {

    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;

    public Member findByMemberId(Long memberId) {
        if (memberId == null) {
            throw new NullPointerException("사용자의 id가 null입니다.");
        }

        return memberRepository.findById(memberId).orElseThrow(() -> new NoSuchElementException("해당하는 사용자가 없습니다."));
    }

    public Notification findByNotificationId(Long notificationId) {
        if (notificationId == null) {
            throw new NullPointerException("알림 id가 null입니다.");
        }

        return notificationRepository.findByNotificationId(notificationId).orElseThrow(() -> new NoSuchElementException("해당하는 알림이 없습니다."));
    }

    @Transactional
    public void deleteNotification(Notification notification) {
        notificationRepository.delete(notification);
    }

    @Transactional
    public Notification saveNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Transactional
    public void createTestMembers(int num) {
        for (int i = 1; i <= num; i++) {
            Member member = memberRepository.save(Member.builder()
                    .userLoginId("user" + i)
                    .memberName("user" + i)
                    .memberEmail("user" + i + "@a.b")
                    .nickname("testMember" + i)
                    .birth(LocalDate.now())
                    .gender(Gender.WOMAN)
                    .passwordHash("hashedPassword")
                    .role(Role.USER)
                    .build());
        }
    }

}
