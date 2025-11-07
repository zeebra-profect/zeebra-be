package com.zeebra.domain.notification;

import com.zeebra.ZeebraApplication;
import com.zeebra.domain.auth.dto.SignupRequest;
import com.zeebra.domain.auth.dto.SignupResponse;
import com.zeebra.domain.auth.service.AuthService;
import com.zeebra.domain.member.entity.Gender;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.notification.dto.NotificationRequest;
import com.zeebra.domain.notification.dto.NotificationResponse;
import com.zeebra.domain.notification.dto.NotificationsResponse;
import com.zeebra.domain.notification.entity.Notification;
import com.zeebra.domain.notification.entity.NotificationType;
import com.zeebra.domain.notification.repository.NotificationRepository;
import com.zeebra.domain.notification.service.NotificationService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest(classes = ZeebraApplication.class)
@Transactional
public class NotificationTest {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private AuthService authService;

    @BeforeEach
    public void truncate() {
        memberRepository.deleteAll();
        notificationRepository.deleteAll();
    }

    @Test
    @DisplayName("TC-IT-NOTI-CREATE-001-[정상] 유효한 memberId와 Type으로 알림 생성 시 DB에 저장됨")
    public void createNotification_validInput_success() {
        // given
        SignupResponse member1 = createTestMember("user1", "user1@abc.a");

        NotificationRequest request = new NotificationRequest(member1.member().memberId(), NotificationType.TEST_OBJECT, new Object());

        // when
        notificationService.createNotification(request);

        // then
        Optional<Notification> savedNotification = notificationRepository.findByNotificationTypeAndMemberId(request.getNotificationType(), request.getMemberId());

        assertThat(savedNotification).isNotNull();
        assertThat(savedNotification.get().getNotificationType()).isEqualTo(request.getNotificationType());
        assertThat(savedNotification.get().getMemberId()).isEqualTo(request.getMemberId());
    }

    @Test
    @DisplayName("TC-IT-NOTI-CREATE-002-[예외] 잘못된 memberId 입력 시 알림 생성 실패")
    public void createNotification_invalidMemberId_fail() {
        // given
        Long invalidMemberId = 99999L;  // 존재하지 않는 memberId
        NotificationRequest request = new NotificationRequest(invalidMemberId, NotificationType.TEST, null);

        // when & then
        assertThatThrownBy(() -> notificationService.createNotification(request)).isInstanceOf(NoSuchElementException.class).hasMessage("해당하는 사용자가 없습니다.");
    }

    @Test
    @DisplayName("TC-IT-NOTI-CREATE-003-[예외] null Type 입력 시 알림 생성 실패")
    public void createNotification_invalidType_fail() {
        // given
        SignupResponse member1 = createTestMember("user1", "user1@abc.a");

        NotificationRequest request = new NotificationRequest(member1.member().memberId(), null, new Object());

        // when & then
        assertThatThrownBy(() -> notificationService.createNotification(request)).isInstanceOf(IllegalArgumentException.class).hasMessage("타입 값이 없습니다.");
    }

    @Test
    @DisplayName("TC-IT-NOTI-CREATE-004-[정상] object, url 포함 알림 생성")
    public void createNotification_validObjectAndUrl_success() {
        // given
        SignupResponse member1 = createTestMember("user1", "user1@abc.a");

        NotificationRequest request = new NotificationRequest(member1.member().memberId(), NotificationType.TEST_OBJECT, new Object());

        // when
        notificationService.createNotification(request);

        // then
        Optional<Notification> savedNotification = notificationRepository.findByNotificationTypeAndMemberId(request.getNotificationType(), request.getMemberId());

        assertThat(savedNotification).isNotNull();
        assertThat(savedNotification.get().getNotificationType()).isEqualTo(request.getNotificationType());
        assertThat(savedNotification.get().getMemberId()).isEqualTo(request.getMemberId());
        assertThat(savedNotification.get().getUrl()).isEqualTo("/dummy");
    }

    @Test
    @DisplayName("TC-IT-NOTI-CREATE-005-[정상] 여러 사용자에게 알림 생성")
    public void createNotification_multipleNotificationsForDifferentMembers_success() {
        // given
        SignupResponse member1 = createTestMember("user1", "user1@abc.a");
        SignupResponse member2 = createTestMember("user2", "user2@abc.a");
        SignupResponse member3 = createTestMember("user3", "user3@abc.a");

        // when
        notificationService.createNotification(new NotificationRequest(member1.member().memberId(), NotificationType.TEST, null));
        notificationService.createNotification(new NotificationRequest(member2.member().memberId(), NotificationType.TEST, null));
        notificationService.createNotification(new NotificationRequest(member3.member().memberId(), NotificationType.TEST, null));

        // then
        assertThat(notificationRepository.findByNotificationTypeAndMemberId(NotificationType.TEST, member1.member().memberId())).isNotNull();
        assertThat(notificationRepository.findByNotificationTypeAndMemberId(NotificationType.TEST, member2.member().memberId())).isNotNull();
        assertThat(notificationRepository.findByNotificationTypeAndMemberId(NotificationType.TEST, member3.member().memberId())).isNotNull();
    }

    @Test
    @DisplayName("TC-UT-NOTI-FIND-001-[정상] 특정 회원의 알림 목록 조회 성공")
    public void getNotifications_validMemberId_success() {
        // given
        SignupResponse member1 = createTestMember("user1", "user1@abc.a");
        notificationService.createNotification(new NotificationRequest(member1.member().memberId(), NotificationType.TEST, null));
        notificationService.createNotification(new NotificationRequest(member1.member().memberId(), NotificationType.TEST, null));
        notificationService.createNotification(new NotificationRequest(member1.member().memberId(), NotificationType.TEST, null));

        // when
        NotificationsResponse response = notificationService.getNotifications(member1.member().memberId());

        // then
        assertThat(response.dtos().size()).isEqualTo(3);
    }

    @Test
    @DisplayName("TC-UT-NOTI-FIND-002-[정상] 알림이 없는 회원 조회 시 빈 목록 반환")
    public void getNotifications_validMemberIdWithEmptyResult_success() {
        // given
        SignupResponse member1 = createTestMember("user1", "user1@abc.a");

        // when
        NotificationsResponse response = notificationService.getNotifications(member1.member().memberId());

        // then
        assertThat(response).isNull();
    }

    @Test
    @DisplayName("TC-UT-NOTI-FIND-003-[예외] 존재하지 않는 회원의 알림 조회 실패")
    public void getNotifications_invalidMemberId_fail() {
        // when & then
        assertThatThrownBy(() -> notificationService.getNotifications(1L)).isInstanceOf(NoSuchElementException.class).hasMessage("해당하는 사용자가 없습니다.");
    }

    @Test
    @DisplayName("TC-UT-NOTI-FIND-004-[예외] null memberId로 알림 조회 실패")
    public void getNotifications_nullMemberId_fail() {
        // when & then
        assertThatThrownBy(() -> notificationService.getNotifications(null)).isInstanceOf(NullPointerException.class).hasMessage("사용자의 id가 null입니다.");
    }

    @Test
    @DisplayName("TC-IT-NOTI-FIND-005-[정상] 특정 알림 단건 조회 성공")
    public void getNotificationById_validNotificationId_success() {
        // given
        SignupResponse member = createTestMember("user1", "user1@abc.a");
        NotificationRequest request = new NotificationRequest(
                member.member().memberId(),
                NotificationType.TEST,
                null
        );
        NotificationResponse createdNotification = notificationService.createNotification(request);

        // when
        NotificationResponse foundNotification = notificationService
                .getNotificationById(createdNotification.notificationId());

        // then
        assertThat(foundNotification).isNotNull();
        assertThat(foundNotification.notificationId()).isEqualTo(createdNotification.notificationId());
        assertThat(foundNotification.memberId()).isEqualTo(member.member().memberId());
        assertThat(foundNotification.notificationType()).isEqualTo(createdNotification.notificationType());
        assertThat(foundNotification.url()).isEqualTo(createdNotification.url());
        assertThat(foundNotification.isRead()).isFalse();
    }

    @Test
    @DisplayName("TC-UT-NOTI-FIND-006-[예외] 존재하지 않는 알림 ID 조회 실패")
    public void getNotificationById_invalidMemberId_fail() {
        // when & then
        assertThatThrownBy(() -> notificationService.getNotificationById(99999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("해당하는 알림이 없습니다.");
    }

    // 헬퍼 메서드
    private SignupResponse createTestMember(String loginId, String email) {
        SignupRequest signupRequest = new SignupRequest(loginId, loginId, email, loginId, "test1234!", "test1234!", LocalDate.now(), Gender.MAN);
        return authService.register(signupRequest);
    }

}