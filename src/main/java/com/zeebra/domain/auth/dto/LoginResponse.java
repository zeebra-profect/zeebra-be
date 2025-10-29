package com.zeebra.domain.auth.dto;

import com.zeebra.domain.member.dto.MemberInfo;

public record LoginResponse(MemberInfo user) {
	public static LoginResponse of(MemberInfo user) {
		return new LoginResponse(user);
	}
}