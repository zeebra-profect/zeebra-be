package com.zeebra.domain.webpush.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebPushRequest {
    private String endpoint;
    private String p256dh;    // keys.p256dh
    private String auth;      // keys.auth
    private String deviceInfo;
}
