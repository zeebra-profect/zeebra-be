package com.zeebra.domain.member.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.zeebra.global.jpa.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@Table(name = "members")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "user_login_id", nullable = false, updatable = false, length = 20)
    private String userLoginId;

    @Column(name = "member_name", nullable = false, updatable = false, length = 20)
    private String memberName;

    @Column(name = "member_email", nullable = false, updatable = false, length = 320)
    private String memberEmail;

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    @Column(name = "member_birth", nullable = false)
    private LocalDate birth;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_gender", length = 10)
    private Gender gender;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 10)
    private Role role;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_bank", length = 20)
    private String accountBank;

    @Column(name = "member_image", length = 2048)
    private String memberImage;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "total_point", nullable = false)
    private Integer totalPoint = 0;

    @Builder
    public Member(String userLoginId, String memberName, String memberEmail, String nickname, LocalDate birth, Gender gender, String passwordHash, Role role, String memberImage, Integer totalPoint) {
        this.userLoginId = userLoginId;
        this.memberName = memberName;
        this.memberEmail = memberEmail;
        this.nickname = nickname;
        this.birth = birth;
        this.gender = gender;
        this.passwordHash = passwordHash;
        this.role = role != null ? role : Role.USER;
        this.memberImage = memberImage;
        this.totalPoint = totalPoint != null ? totalPoint : 0;
    }

    public static Member createMember(String userLoginId, String memberName, String memberEmail, String nickname, LocalDate birth, Gender gender, String passwordHash) {
        return Member.builder()
                .userLoginId(userLoginId)
                .memberName(memberName)
                .memberEmail(memberEmail)
                .nickname(nickname)
                .birth(birth)
                .gender(gender)
                .passwordHash(passwordHash)
                .role(Role.USER)
                .build();
    }

    private static String normalizeEmail(String email) {
        return (email == null) ? null : email.trim().toLowerCase();
    }

    @PrePersist
    @PreUpdate
    private void onPersistOrUpdate() {
        this.memberEmail = normalizeEmail(this.memberEmail);
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public boolean isActive() {
        return this.deletedAt == null;
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }
}