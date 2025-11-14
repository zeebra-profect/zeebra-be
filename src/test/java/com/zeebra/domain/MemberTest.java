package com.zeebra.domain;

import com.zeebra.domain.member.entity.Gender;
import com.zeebra.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.zeebra.domain.member.entity.Member.createAdmin;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class MemberTest {


    @DisplayName("관리자가 아니라면 오류를 던진다")
    @Test
    void validateAdminPermission_WhenNotAdmin_ThrowsException() {
        // given
        Member member = Member.createMember("testUser123", "홍길동", "hong@test.com",
                "길동이", LocalDate.of(1990, 5, 15), Gender.MAN, "hashedPassword123");
        // when & then
        assertThatThrownBy(() -> member.validateAdminPermission())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("관리자가 아닙니다.");
    }


    @DisplayName("관리자는 권한 검증을 통과한")
    @Test
    void validateAdminPermission_Succes() {
        // given
        Member admin = createAdmin("testUser123", "홍길동", "hong@test.com",
                "길동이", LocalDate.of(1990, 5, 15), Gender.MAN, "hashedPassword123");
        // when
        assertThatCode(() -> admin.validateAdminPermission())
                .doesNotThrowAnyException();

        // then


    }
}
