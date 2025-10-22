package com.zeebra.domain.product.repository;

import com.zeebra.domain.product.entity.FavoriteProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteProductRepository extends JpaRepository<FavoriteProduct, Long> {
}
