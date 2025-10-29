package com.zeebra.domain.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeebra.domain.member.dto.MemberInfo;
import com.zeebra.domain.member.dto.MemberProfile;
import com.zeebra.domain.member.service.MemberService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Member API", description = "회원 관련 API")
public class MemberController {

	private final MemberService memberService;

	@Operation(summary = "나의 정보 조회 API")
	@GetMapping("/me")
	public ApiResponse<MemberInfo> getMyInfo(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal){

		Long memberId = principal.getMemberId();

		return ApiResponse.success(memberService.findById(memberId));
	}

	@Operation(summary = "타인 프로필 조회 API")
	@GetMapping("/{memberId}")
	public ApiResponse<MemberProfile> getMemberProfile(@PathVariable Long memberId){
		MemberInfo member = memberService.findById(memberId);

		return ApiResponse.success(MemberProfile.of(member));
	}

	// @GetMapping("/me")
	// @PatchMapping("/nickname")
	// @DeleteMapping
}