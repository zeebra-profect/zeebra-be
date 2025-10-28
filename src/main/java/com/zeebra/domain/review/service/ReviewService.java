package com.zeebra.domain.review.service;

import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.product.entity.ProductOption;
import com.zeebra.domain.product.repository.ProductOptionRepository;
import com.zeebra.domain.review.dto.ReviewDelReq;
import com.zeebra.domain.review.dto.ReviewFindReq;
import com.zeebra.domain.review.dto.ReviewRequest;
import com.zeebra.domain.review.dto.ReviewResponse;
import com.zeebra.domain.review.entity.Review;
import com.zeebra.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final ProductOptionRepository productOptionRepository;

    public ReviewResponse createReview(Long memberId, Long productOptionId, ReviewRequest request) {

        ProductOption productOption = productOptionRepository.findById(productOptionId).orElseThrow(
                () -> new NoSuchElementException("해당하는 상품 옵션이 없습니다."));

        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new NoSuchElementException("해당하는 사용자가 없습니다."));
        Review review = new Review(member.getId(), productOption.getId(), request.images(), request.content());
        reviewRepository.save(review);

        // 이미지 넣기 추가하기 (추후에)
        return new ReviewResponse (review.getId(),member.getId(),review.getProductOptionId(), review.getImages(), review.getContent(), review.getLikeCount(), review.getCreatedTime());
    }

    //리뷰 상세 조회
    //1. 파라미터를 정의한다
    //2. 리뷰 리포지토리(테이블)에서 reviewid기반으로 단건 조회한다.
    //select * from review where id = 10;
    //3. 리뷰를 리뷰 응답 dto로 변환해서 리턴한다.

    public ReviewResponse findReview(ReviewFindReq dto){

        Review review = reviewRepository.findById(dto.reviewId()).orElseThrow(
                () -> new NoSuchElementException("해당하는 리뷰가 없습니다."));

        //토큰 검증하고 온거면 이미 유저가 잇다고 식별이된거에요
        Member member = memberRepository.findById(dto.memberId()).orElseThrow(
                () -> new NoSuchElementException("해당하는 유저가 없습니다."));

        return new ReviewResponse(dto.reviewId(),dto.memberId(), review.getProductOptionId(), review.getImages(), review.getContent(), review.getLikeCount(), review.getCreatedTime());
    }

    @Transactional
    public void deleteReview(Long reviewId, Long memberId) {

        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new NoSuchElementException("해당하는 리뷰가 없습니다."));

        boolean isOwner = review.getMemberId().equals(memberId);
        if (!isOwner) {
            throw new org.springframework.security.access.AccessDeniedException("리뷰 삭제 권한이 없습니다.");
        }

        reviewRepository.deleteById(reviewId);
    }

}
