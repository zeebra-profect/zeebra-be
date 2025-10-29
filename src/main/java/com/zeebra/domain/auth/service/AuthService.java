package com.zeebra.domain.auth.service;

import com.zeebra.domain.auth.dto.*;

public interface AuthService {

    SignupResponse register(SignupRequest request);

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

    MemberInfo findById(Long id);
}