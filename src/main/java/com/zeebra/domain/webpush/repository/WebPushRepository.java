package com.zeebra.domain.webpush.repository;

import com.zeebra.domain.webpush.entity.WebPush;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebPushRepository extends JpaRepository<WebPush, Long> {
    void deleteByMemberId(Long memberId);

    Optional<WebPush> findByMemberId(Long memberId);  // 푸시 발송용

    Optional<WebPush> findByMemberIdAndDeviceInfo(Long memberId, String deviceInfo);

}
