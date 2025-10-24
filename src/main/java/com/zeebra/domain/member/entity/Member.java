//package com.zeebra.domain.member.entity;
//
//import com.zeebra.global.jpa.BaseEntity;
//import jakarta.persistence.*;
//import lombok.*;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.Collection;
//import java.util.List;
//
//@Entity
//@Getter
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@AllArgsConstructor
//@Builder
//@EqualsAndHashCode(of = "id")
//@Table(
//        name = "members",
//        uniqueConstraints = {
//                @UniqueConstraint(name = "uk_member_user_login_id", columnNames = "user_login_id"),
//                @UniqueConstraint(name = "uk_member_email", columnNames = "member_email")
//        }
//)
//public class Member extends BaseEntity implements UserDetails {
//
//    // PK: member_id Long
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "member_id", nullable = false, updatable = false)
//    private Long id;
//
//    /** 로그인 아이디(별도 식별자) - 기존 로직 유지용 */
//    @Column(name = "user_login_id", nullable = false, unique = true, length = 20)
//    private String userLoginId;
//
//    /** member_name varchar */
//    @Column(name = "member_name", nullable = false, length = 20)
//    private String memberName;
//
//    /** member_email varchar */
//    @Column(name = "member_email", nullable = false, length = 80)
//    private String memberEmail;
//
//    /** nickname varchar */
//    @Column(name = "nickname", nullable = false, length = 20)
//    private String nickname;
//
//    /** member_birth date */
//    @Column(name = "member_birth", nullable = false)
//    private LocalDate birth;
//
//    /** member_gender enum(man, woman) */
//    public enum Gender { MAN, WOMAN }
//    @Enumerated(EnumType.STRING)
//    @Column(name = "member_gender", length = 10)
//    private Gender gender;
//
//    /** password varchar -> 해시 보관, 컬럼명은 password */
//    @Column(name = "password", nullable = false, length = 255)
//    private String passwordHash;
//
//    /** member_role enum(user, admin) */
//    public enum Role { USER, ADMIN }
//    @Enumerated(EnumType.STRING)
//    @Column(name = "member_role", nullable = false, length = 10)
//    @Builder.Default
//    private Role role = Role.USER;
//
//    /** account_number varchar */
//    @Column(name = "account_number", length = 16)
//    private String accountNumber;
//
//    /** account_bank varchar */
//    @Column(name = "account_bank", length = 20)
//    private String accountBank;
//
//    /** member_image varchar (URL) */
//    @Column(name = "member_image", length = 255)
//    private String memberImage; // 이미지 주소(URL)
//
//    /** deleted_at date (soft delete용) */
//    @Column(name = "deleted_at")
//    private LocalDateTime deletedAt;
//
//    /** create_at date */
//    @Column(name = "create_at", nullable = false)
//    @Builder.Default
//    private LocalDate createdAt = LocalDate.now();
//
//    /** total_point int */
//    @Column(name = "total_point", nullable = false)
//    @Builder.Default
//    private Integer totalPoint = 0;
//
//    /* ===== UserDetails 구현 ===== */
//
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        // 단일 Role 기반으로 GrantedAuthority 생성
//        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
//    }
//
//    @Override
//    public String getPassword() { return this.passwordHash; }
//
//    @Override
//    public String getUsername() { return this.userLoginId; } // 로그인 식별자는 userLoginId 유지
//
//    @Override public boolean isAccountNonExpired() { return true; }
//    @Override public boolean isAccountNonLocked() { return true; }
//    @Override public boolean isCredentialsNonExpired() { return true; }
//    @Override public boolean isEnabled() { return deletedAt == null; } // 삭제(비활) 시 false 처리도 가능
//}
