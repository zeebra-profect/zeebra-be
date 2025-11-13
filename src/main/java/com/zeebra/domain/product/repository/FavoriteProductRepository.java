package com.zeebra.domain.product.repository;

import com.zeebra.domain.product.entity.FavoriteProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteProductRepository extends JpaRepository<FavoriteProduct, Long> {

    Optional<FavoriteProduct> findByMemberIdAndProductId(Long memberId, Long productId);
}
