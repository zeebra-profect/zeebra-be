package com.zeebra.domain.auth.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.zeebra.domain.auth.dto.LoginRequest;
import com.zeebra.domain.auth.dto.LoginSuccess;
import com.zeebra.domain.member.dto.MemberInfo;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.global.ErrorCode.AuthErrorCode;
import com.zeebra.global.exception.BusinessException;
import com.zeebra.global.security.jwt.JwtProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;

	public LoginSuccess login(LoginRequest request){

		Optional<Member> optional = memberRepository.findByUserLoginIdAndDeletedAtIsNull(request.identifier());
		Member member = optional.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

		if(!member.isActive()){
			throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
		}

		if(!passwordEncoder.matches(request.password(), member.getPasswordHash())){
			throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
		}

		Long accessTokenMinutes = 60L;
		Long refreshTokenDays = 7L;

		String accessToken = jwtProvider.createAccessToken(member.getId(), member.getUserLoginId(),member.getRole().toString(), accessTokenMinutes);
		String refreshToken = jwtProvider.createRefreshToken(member.getId(), member.getRole().toString(), refreshTokenDays);

		MemberInfo memberInfo = MemberInfo.of(member);

		return new LoginSuccess(accessToken, refreshToken, accessTokenMinutes, refreshTokenDays, memberInfo);
	}
}