package com.zeebra.domain.member.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.zeebra.domain.member.entity.Gender;
import com.zeebra.domain.member.entity.Member;

public record SignupResponse(MemberInfo member) {
	public static SignupResponse of(Member member) {
		return new SignupResponse(MemberInfo.of(member));
	}

	public record MemberInfo(Long memberId, String userLoginId, String memberName, String memberEmail, String nickname, Gender gender, LocalDate memberBirth, LocalDateTime createdTime) {
		public static MemberInfo of(Member member) {
			return new MemberInfo(member.getId(), member.getUserLoginId(), member.getMemberName(), member.getMemberEmail(), member.getNickname(), member.getGender(), member.getBirth(), member.getCreatedTime());
		}
	}
}