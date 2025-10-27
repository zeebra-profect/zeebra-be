package com.zeebra.domain.auth.service;

import com.zeebra.domain.auth.dto.LoginRequest;
import com.zeebra.domain.auth.dto.LoginSuccess;

public interface AuthService {
	LoginSuccess login(LoginRequest request);

	void logout(String accessToken, String refreshToken);
	// public boolean isLogin();
	// public String getEmail();
	// public String getNickname();
	// public String getProfileImageUrl();
	// public String getAccessToken();
	// public String getRefreshToken();
	// public Long getMemberId();
	// public String getMemberType();
}