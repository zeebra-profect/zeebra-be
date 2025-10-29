package com.zeebra.domain.auth.dto;

import com.zeebra.domain.member.entity.Member;

public record SignupResponse(MemberInfo member) {
	public static SignupResponse of(Member member) {
		return new SignupResponse(MemberInfo.of(member));
	}
}