package com.zeebra.domain.auth.dto;

import com.zeebra.domain.member.dto.MemberInfo;

public record LoginSuccess(
	String accessToken,
   	String refreshToken,
   	long accessTokenTtlMinutes,
   	long refreshTokenTtlDays,
   	MemberInfo memberInfo
) {}