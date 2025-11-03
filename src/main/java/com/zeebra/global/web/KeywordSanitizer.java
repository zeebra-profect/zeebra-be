package com.zeebra.global.web;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.text.Normalizer;
import java.util.regex.Pattern;

@NoArgsConstructor
public final class KeywordSanitizer {
    private static final Pattern CONTROL = Pattern.compile("\\p{C}+"); // 제어문자 전부
    private static final Pattern MULTI_WS = Pattern.compile("\\s{2,}");


    public static String sanitize(String raw) {
        if (raw == null) return null;
        String s = Normalizer.normalize(raw, Normalizer.Form.NFC);
        s = CONTROL.matcher(s).replaceAll(" ");      // 제어문자 → 공백
        s = MULTI_WS.matcher(s.trim()).replaceAll(" "); // 다중 공백 축약
        return s;
    }
}
