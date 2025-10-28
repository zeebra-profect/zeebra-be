package com.zeebra.domain.review.controller;

import com.zeebra.domain.review.dto.ReviewFindReqDto;
import com.zeebra.domain.review.dto.ReviewLikeReqDto;
import com.zeebra.domain.review.dto.ReviewRequest;
import com.zeebra.domain.review.dto.ReviewResponse;
import com.zeebra.domain.review.entity.Review;
import com.zeebra.domain.review.service.ReviewService;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    //등록(POST) -> HTTP BODY, ReqParam/PathVariable
    @PostMapping("/api/reviews/{productOptionId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal,
                                       @PathVariable Long productOptionId, @RequestBody ReviewRequest request) {

        Long memberId = principal.getMemberId();
        return ResponseEntity.created(null).body(ApiResponse.success(reviewService.createReview(memberId, productOptionId, request)));

    }

    //리뷰 상세 조회(GET), ReqParam/PathVariable
    @GetMapping("/api/reviews/{reviewId}")

    public ApiResponse<ReviewResponse> retrieveReview(@AuthenticationPrincipal JwtProvider.JwtUserPrincipal principal, @PathVariable Long reviewId){
        Long memberId = principal.getMemberId();
        ReviewFindReqDto request = new ReviewFindReqDto(reviewId, memberId);
        ApiResponse<ReviewResponse> result = new ApiResponse<ReviewResponse>();
        return ApiResponse.success(reviewService.findReview(request));
    }


    //리뷰 목록 조회
}


