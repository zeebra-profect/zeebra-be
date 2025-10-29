package com.zeebra.domain.member.dto;

public record MemberProfile(
	Long memberId,
	String nickname,
	String profileImage
) {
	public static MemberProfile of(MemberInfo member) {
		return new MemberProfile(member.memberId(), member.nickname(), member.memberImage());
	}
}