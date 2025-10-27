package com.zeebra.domain.review.service;

import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.product.entity.ProductOption;
import com.zeebra.domain.product.repository.ProductOptionRepository;
import com.zeebra.domain.review.dto.ReviewRequest;
import com.zeebra.domain.review.dto.ReviewResponse;
import com.zeebra.domain.review.entity.Review;
import com.zeebra.domain.review.repository.ReviewLikeRepository;
import com.zeebra.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

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
        return new ReviewResponse (review.getId(),member.getId(),review.getImages(),review.getContent(),review.getCreatedTime());
    }

}
