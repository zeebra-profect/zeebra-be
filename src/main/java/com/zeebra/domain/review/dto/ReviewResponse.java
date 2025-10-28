package com.zeebra.domain.review.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
    Long reviewId,
    Long memberId,
    Long productOptionId,
    List<String> images,
    String content,
    Integer likeCount,
    LocalDateTime createdAt
) {

}
