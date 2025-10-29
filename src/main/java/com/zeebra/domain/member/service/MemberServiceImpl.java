package com.zeebra.domain.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zeebra.domain.member.dto.MemberInfo;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.global.ErrorCode.MemberErrorCode;
import com.zeebra.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService{

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public MemberInfo findById(Long memberId) {
 		Member member =  memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

		 return MemberInfo.of(member);
	}


	// public void updateNickname(String nickname) {}
	// public void updatePassword(String password) {}
	// public void deleteAccount() {}
}