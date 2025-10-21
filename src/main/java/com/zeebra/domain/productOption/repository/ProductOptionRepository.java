package com.zeebra.domain.productOption.repository;

import com.zeebra.domain.productOption.entity.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {
}
