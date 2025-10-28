package com.zeebra.domain.member.service;

import com.zeebra.domain.member.dto.SignupRequest;
import com.zeebra.domain.member.dto.SignupResponse;

public interface MemberService {
	SignupResponse register(SignupRequest request);
	// public void login(String email, String password);
	// public void logout();
	// public void updateNickname(String nickname);
	// public void updatePassword(String password);
	// public void deleteAccount();
}