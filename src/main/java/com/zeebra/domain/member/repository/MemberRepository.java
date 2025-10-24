package com.zeebra.domain.member.repository;

import com.zeebra.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // ✅ 로그인 아이디 중복 체크 (회원가입 시)
    boolean existsByUserLoginId(String userLoginId);

    // ✅ 이메일 중복 체크 (선택 — 회원가입 시 이메일도 유니크라면)
    boolean existsByMemberEmail(String memberEmail);

    // ✅ 로그인 시 사용자 조회용
    Optional<Member> findByUserLoginId(String userLoginId);

    // ✅ 이메일로 찾기 (비밀번호 재설정 같은 용도로)
    Optional<Member> findByMemberEmail(String memberEmail);
}
