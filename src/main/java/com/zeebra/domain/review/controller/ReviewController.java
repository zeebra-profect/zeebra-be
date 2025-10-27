package com.zeebra.domain.review.controller;

import com.zeebra.domain.review.dto.ReviewRequest;
import com.zeebra.domain.review.dto.ReviewResponse;
import com.zeebra.domain.review.service.ReviewService;
import com.zeebra.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/reviews/{productOptionId}")
    public ReviewResponse createReview(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal,
                                       @PathVariable Long productOptionId, @RequestBody ReviewRequest request) {
        Long memberId = principal.getMemberId();
        return reviewService.createReview(memberId, productOptionId, request);

    }

}


