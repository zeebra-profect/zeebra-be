package com.zeebra.domain.auth.dto;

public record LoginResponse(MemberInfo user) {
	public static LoginResponse of(MemberInfo user) {
		return new LoginResponse(user);
	}
}