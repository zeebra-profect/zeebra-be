package com.zeebra.domain.notification;

import com.zeebra.domain.member.entity.Gender;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.entity.Role;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.notification.dto.NotificationRequest;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.dto.NotificationsResponse;
import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.notification.entity.NotificationType;
import com.zeebra.domain.notification.repository.NotificationRepository;
import com.zeebra.domain.notification.service.NotificationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationUTTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("TC-UT-NOTI-001-[정상] 유효한 memberId와 Type으로 알림 생성 시 저장 호출")
    public void createNotification_validInput_success() {
        // given
        Member mockMember = createMockMember(1L, "user1", "user1@abc.a");
        NotificationRequest request = new NotificationRequest(1L, NotificationType.TEST, null);

        Notification mockNotification = createMockNotification(1L, 1L, NotificationType.TEST, "/dummy/url");

        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember));
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        // when
        NotificationResponse response = notificationService.createNotification(request);

        // then
        verify(memberRepository).findById(1L);
        verify(notificationRepository).save(any(Notification.class));
        assertThat(response).isNotNull();
        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.notificationType()).isEqualTo(NotificationType.TEST);
    }

    @Test
    @DisplayName("TC-UT-NOTI-002-[예외] 잘못된 memberId 입력 시 알림 생성 실패")
    public void createNotification_invalidMemberId_fail() {
        // given
        Long invalidMemberId = 99999L;
        NotificationRequest request = new NotificationRequest(invalidMemberId, NotificationType.TEST, null);

        when(memberRepository.findById(invalidMemberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.createNotification(request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("해당하는 사용자가 없습니다.");

        verify(memberRepository).findById(invalidMemberId);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-UT-NOTI-003-[예외] null Type 입력 시 알림 생성 실패")
    public void createNotification_invalidType_fail() {
        // given
        Member mockMember = createMockMember(1L, "user1", "user1@abc.a");
        NotificationRequest request = new NotificationRequest(1L, null, null);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember));

        // when & then
        assertThatThrownBy(() -> notificationService.createNotification(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("타입 값이 없습니다.");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-UT-NOTI-004-[정상] object, url 포함 알림 생성")
    public void createNotification_validObjectAndUrl_success() {
        // given
        Member mockMember = createMockMember(1L, "user1", "user1@abc.a");
        NotificationRequest request = new NotificationRequest(1L, NotificationType.TEST_OBJECT, new Object());

        Notification mockNotification = createMockNotification(1L, 1L, NotificationType.TEST_OBJECT, "/dummy/testId");

        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember));
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        // when
        NotificationResponse response = notificationService.createNotification(request);

        // then
        verify(notificationRepository).save(any(Notification.class));
        assertThat(response).isNotNull();
        assertThat(response.notificationType()).isEqualTo(NotificationType.TEST_OBJECT);
        assertThat(response.url()).isEqualTo("/dummy/testId");
    }

    @Test
    @DisplayName("TC-UT-NOTI-005-[정상] 여러 사용자에게 알림 생성")
    public void createNotification_multipleNotificationsForDifferentMembers_success() {
        // given
        Member mockMember1 = createMockMember(1L, "user1", "user1@abc.a");
        Member mockMember2 = createMockMember(2L, "user2", "user2@abc.a");
        Member mockMember3 = createMockMember(3L, "user3", "user3@abc.a");

        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember1));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(mockMember2));
        when(memberRepository.findById(3L)).thenReturn(Optional.of(mockMember3));

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(createMockNotification(1L, 1L, NotificationType.TEST_OBJECT, "/dummy/url"))
                .thenReturn(createMockNotification(2L, 2L, NotificationType.TEST_OBJECT, "/dummy/url"))
                .thenReturn(createMockNotification(3L, 3L, NotificationType.TEST_OBJECT, "/dummy/url"));

        // when
        notificationService.createNotification(new NotificationRequest(1L, NotificationType.TEST, new Object()));
        notificationService.createNotification(new NotificationRequest(2L, NotificationType.TEST, new Object()));
        notificationService.createNotification(new NotificationRequest(3L, NotificationType.TEST, new Object()));

        // then
        verify(notificationRepository, times(3)).save(any(Notification.class));
    }

    @Test
    @DisplayName("TC-UT-NOTI-006-[정상] 특정 회원의 알림 목록 조회 성공")
    public void getNotifications_validMemberId_success() {
        // given
        Member mockMember = createMockMember(1L, "user1", "user1@abc.a");

        List<Optional<Notification>> mockNotifications = Arrays.asList(
                Optional.of(createMockNotification(1L, 1L, NotificationType.TEST_OBJECT, "/dummy/url")),
                Optional.of(createMockNotification(2L, 1L, NotificationType.TEST_OBJECT, "/dummy/url")),
                Optional.of(createMockNotification(3L, 1L, NotificationType.TEST_OBJECT, "/dummy/url"))
        );

        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember));
        when(notificationRepository.findByMemberIdOrderByCreatedTimeDesc(1L))
                .thenReturn(mockNotifications);

        // when
        NotificationsResponse response = notificationService.getNotifications(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.dtos()).isNotNull();
        assertThat(response.dtos().size()).isEqualTo(3);

        assertThat(response.dtos().get(0).notificationId()).isEqualTo(1L);
        assertThat(response.dtos().get(0).memberId()).isEqualTo(1L);
        assertThat(response.dtos().get(0).notificationType()).isEqualTo(NotificationType.TEST_OBJECT);
        assertThat(response.dtos().get(1).notificationId()).isEqualTo(2L);
        assertThat(response.dtos().get(1).memberId()).isEqualTo(1L);
        assertThat(response.dtos().get(1).notificationType()).isEqualTo(NotificationType.TEST_OBJECT);
        assertThat(response.dtos().get(2).notificationId()).isEqualTo(3L);
        assertThat(response.dtos().get(2).memberId()).isEqualTo(1L);
        assertThat(response.dtos().get(2).notificationType()).isEqualTo(NotificationType.TEST_OBJECT);
    }

    @Test
    @DisplayName("TC-UT-NOTI-007-[정상] 알림이 없는 회원 조회 시 빈 목록 반환")
    public void getNotifications_validMemberIdWithEmptyResult_success() {
        // given
        Member mockMember = createMockMember(1L, "user1", "user1@abc.a");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(mockMember));

        // when
        NotificationsResponse response = notificationService.getNotifications(1L);

        // then
        assertThat(response).isNull();
    }

    @Test
    @DisplayName("TC-UT-NOTI-008-[예외] 존재하지 않는 회원의 알림 조회 실패")
    public void getNotifications_invalidMemberId_fail() {
        // given
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.getNotifications(1L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("해당하는 사용자가 없습니다.");

        verify(notificationRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("TC-UT-NOTI-009-[예외] null memberId로 알림 조회 실패")
    public void getNotifications_nullMemberId_fail() {
        // when & then
        assertThatThrownBy(() -> notificationService.getNotifications(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("사용자의 id가 null입니다.");
    }

    @Test
    @DisplayName("TC-UT-NOTI-010-[정상] 특정 알림 단건 조회 성공")
    public void getNotificationById_validNotificationId_success() {
        // given
        Member mockMember = createMockMember(1L, "user1", "user1@abc.a");
        Optional<Notification> mockNotification = Optional.of(createMockNotification(1L, mockMember.getId(), NotificationType.TEST, null));
        doReturn(mockNotification)
                .when(notificationRepository)
                .findByNotificationId(1L);

        // when
        NotificationResponse foundNotification = notificationService.getNotificationById(1L);

        // then
        verify(notificationRepository).findByNotificationId(1L);
        assertThat(foundNotification).isNotNull();
        assertThat(foundNotification.notificationId()).isEqualTo(1L);
        assertThat(foundNotification.memberId()).isEqualTo(1L);
        assertThat(foundNotification.notificationType()).isEqualTo(NotificationType.TEST);
        assertThat(foundNotification.isRead()).isFalse();
    }

    @Test
    @DisplayName("TC-UT-NOTI-011-[예외] 존재하지 않는 알림 ID 조회 실패")
    public void getNotificationById_invalidMemberId_fail() {
        // when & then
        assertThatThrownBy(() -> notificationService.getNotificationById(99999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("해당하는 알림이 없습니다.");
        verify(notificationRepository).findByNotificationId(99999L);
    }


    // 헬퍼 메서드
    private Member createMockMember(Long id, String loginId, String email) {
        Member member = Member.builder()
                .userLoginId(loginId)
                .memberName(loginId)
                .memberEmail(email)
                .nickname("testMember")
                .birth(LocalDate.now())
                .gender(Gender.WOMAN)
                .passwordHash("hashedPassword")
                .role(Role.USER)
                .build();

        // Reflection으로 ID 설정 (또는 mock 사용)
        try {
            java.lang.reflect.Field idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, id);
        } catch (Exception e) {
            // Mock 사용 시 무시
        }

        return member;
    }

    private Notification createMockNotification(Long id, Long memberId, NotificationType type, String url) {
        Notification notification = new Notification(memberId, type, url);

        try {
            java.lang.reflect.Field idField = Notification.class.getDeclaredField("notificationId");
            idField.setAccessible(true);
            idField.set(notification, id);

            java.lang.reflect.Field urlField = Notification.class.getDeclaredField("url");
            urlField.setAccessible(true);
            urlField.set(notification, url);

            java.lang.reflect.Field createdTimeField = Notification.class.getDeclaredField("createdTime");
            createdTimeField.setAccessible(true);
            createdTimeField.set(notification, LocalDateTime.now());
        } catch (Exception e) {
            // 무시
        }

        return notification;
    }
}