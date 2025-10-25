package com.zeebra.domain.member.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zeebra.domain.member.dto.SignupRequest;
import com.zeebra.domain.member.dto.SignupResponse;
import com.zeebra.domain.member.service.MemberService;
import com.zeebra.global.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/members")
public class MemberController {

	private MemberService memberService;

	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody @Valid SignupRequest request) {



		return ResponseEntity.created(null).body(ApiResponse.success(memberService.register(request)));
	}
}