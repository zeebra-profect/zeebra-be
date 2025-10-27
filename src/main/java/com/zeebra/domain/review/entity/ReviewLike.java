package com.zeebra.domain.review.entity;

import com.zeebra.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class ReviewLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reviewId;

    private Long memberId;

    public ReviewLike(Long reviewId, Long memberId) {
        this.reviewId = reviewId;
        this.memberId = memberId;
    }
}
