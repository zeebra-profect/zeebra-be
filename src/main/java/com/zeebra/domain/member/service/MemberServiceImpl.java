package com.zeebra.domain.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zeebra.domain.member.dto.SignupRequest;
import com.zeebra.domain.member.dto.SignupResponse;
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
	public SignupResponse register(SignupRequest request) {

		String email = request.memberEmail().trim().toLowerCase();

		if(memberRepository.existsByUserLoginIdAndDeletedAtIsNull(request.userLoginId())){
			throw new BusinessException(MemberErrorCode.DUPLICATE_LOGIN_ID);
		}

		if(memberRepository.existsByMemberEmailAndDeletedAtIsNull(email)){
			throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL);
		}

		if(memberRepository.existsByNicknameAndDeletedAtIsNull(request.nickname())){
			throw new BusinessException(MemberErrorCode.DUPLICATE_NICKNAME);
		}

		String rawPassword = request.password();

		String encodedPassword = passwordEncoder.encode(rawPassword);

		Member member = Member.createMember(request.userLoginId(), request.memberName(), request.memberEmail(),
			request.nickname(), request.memberBirth(), request.memberGender(), encodedPassword);

		Member saved = memberRepository.save(member);

		return SignupResponse.of(saved);
	}


	// public void login(String email, String password) {}
	// public void logout() {}
	// public void updateNickname(String nickname) {}
	// public void updatePassword(String password) {}
	// public void deleteAccount() {}
}