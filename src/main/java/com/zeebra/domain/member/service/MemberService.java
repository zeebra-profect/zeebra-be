package com.zeebra.domain.member.service;

import com.zeebra.domain.member.entity.Member;

public interface MemberService {
	Member findById(Long memberId);
	// 향후 추가될 메소드들
	// MemberDetailResponse getMemberInfo(Long memberId);
	// void updateNickname(Long memberId, String nickname);
	// void updatePassword(Long memberId, String oldPassword, String newPassword);
	// void deleteAccount(Long memberId);
}