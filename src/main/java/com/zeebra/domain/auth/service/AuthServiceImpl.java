package com.zeebra.domain.auth.service;

import java.util.Date;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zeebra.domain.auth.dto.LoginRequest;
import com.zeebra.domain.auth.dto.LoginSuccess;
import com.zeebra.domain.auth.dto.SignupRequest;
import com.zeebra.domain.auth.dto.SignupResponse;
import com.zeebra.domain.member.dto.MemberInfo;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.notification.event.MemberSignUpEvent;
import com.zeebra.global.ErrorCode.AuthErrorCode;
import com.zeebra.global.ErrorCode.MemberErrorCode;
import com.zeebra.global.exception.BusinessException;
import com.zeebra.global.redis.RedisService;
import com.zeebra.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisService redisService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SignupResponse register(SignupRequest request) {

        String email = request.memberEmail().trim().toLowerCase();

        if (memberRepository.existsByUserLoginIdAndDeletedAtIsNull(request.userLoginId())) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_LOGIN_ID);
        }

        if (memberRepository.existsByMemberEmailAndDeletedAtIsNull(email)) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL);
        }

        if (memberRepository.existsByNicknameAndDeletedAtIsNull(request.nickname())) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_NICKNAME);
        }

        String rawPassword = request.password();

        String encodedPassword = passwordEncoder.encode(rawPassword);

        Member member = Member.createMember(
                request.userLoginId(),
                request.memberName(),
                request.memberEmail(),
                request.nickname(),
                request.memberBirth(),
                request.memberGender(),
                encodedPassword);

        Member saved = memberRepository.save(member);
        eventPublisher.publishEvent(new MemberSignUpEvent(member.getId(), member.getNickname()));

        return SignupResponse.of(saved);
    }

    @Transactional
    public LoginSuccess login(LoginRequest request) {

        Optional<Member> optional = memberRepository.findByUserLoginIdAndDeletedAtIsNull(request.identifier());
        Member member = optional.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        if (!member.isActive()) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        Long accessTokenMinutes = 60L;
        Long refreshTokenDays = 7L;

        String accessToken = jwtProvider.createAccessToken(member.getId(), member.getUserLoginId(), member.getRole().toString(), accessTokenMinutes);
        String refreshToken = jwtProvider.createRefreshToken(member.getId(), member.getRole().toString(), refreshTokenDays);

        MemberInfo memberInfo = MemberInfo.of(member);

        return new LoginSuccess(accessToken, refreshToken, accessTokenMinutes, refreshTokenDays, memberInfo);
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {

        if (accessToken == null && refreshToken == null) {
            throw new BusinessException(AuthErrorCode.AUTH_REQUIRED);
        }

        addTokenToBlacklist(accessToken);

        addTokenToBlacklist(refreshToken);
    }

    private void addTokenToBlacklist(String token) {
        if (token == null || !jwtProvider.isValid(token)) {
            return;
        }

        Date expirationDate = jwtProvider.getExpirationDate(token);
        if (expirationDate == null || expirationDate.getTime() <= System.currentTimeMillis()) {
            return;
        }

        try {
            redisService.addToBlacklist(token, expirationDate.getTime(), jwtProvider.getSubjectAsLong(token));
        } catch (Exception e) {
        }
    }

    public MemberInfo findById(Long id) {
        memberRepository.findById(id);
        return MemberInfo.of(memberRepository.findById(id).get());
    }

    ;
}