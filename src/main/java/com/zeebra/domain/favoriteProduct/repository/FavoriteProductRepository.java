package com.zeebra.domain.favoriteProduct.repository;

import com.zeebra.domain.favoriteProduct.entity.FavoriteProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteProductRepository extends JpaRepository<FavoriteProduct, Long> {
}
