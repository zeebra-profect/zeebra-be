package com.zeebra.domain.member.dto;

import com.zeebra.domain.member.entity.Member;

public record MemberProfile(
	Long memberId,
	String nickname,
	String profileImage
) {
	public static MemberProfile of(Member member) {
		return new MemberProfile(member.getId(), member.getNickname(), member.getMemberImage());
	}
}