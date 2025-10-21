package com.zeebra.domain.reviewLike.repository;

import com.zeebra.domain.reviewLike.entity.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {
}
