package com.zeebra.domain.webpush.entity;

import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "web_push")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WebPush extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long webPushId;
    private Long memberId;
    private String endpoint;  // 푸시 발송 URL
    private String p256dh;    // 암호화 공개키
    private String auth;      // 인증 비밀키
    @Column
    private String deviceInfo;  // Optional: "Chrome on Windows" 같은 정보
}
