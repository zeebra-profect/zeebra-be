package com.zeebra.domain.auth.dto;

public record LoginSuccess(
	String accessToken,
   	String refreshToken,
   	long accessTokenTtlMinutes,
   	long refreshTokenTtlDays,
   	MemberInfo memberInfo
) {}