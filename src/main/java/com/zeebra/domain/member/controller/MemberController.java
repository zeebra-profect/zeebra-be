package com.zeebra.domain.member.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeebra.domain.member.service.MemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	// 향후 회원 정보 조회, 수정 등의 엔드포인트가 여기에 추가됩니다
	// @GetMapping("/me")
	// @PatchMapping("/nickname")
	// @DeleteMapping
}