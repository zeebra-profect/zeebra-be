package com.zeebra.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zeebra.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByUserLoginIdAndDeletedAtIsNull(String userLoginId);

	boolean existsByMemberEmailAndDeletedAtIsNull(String memberEmail);

	boolean existsByNicknameAndDeletedAtIsNull(String nickname);

	Optional<Member> findByUserLoginIdAndDeletedAtIsNull(String userLoginId);

	Optional<Member> findByIdAndDeletedAtIsNull(Long id);
}