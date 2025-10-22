package com.zeebra.domain.product.repository;

import com.zeebra.domain.product.entity.Sales;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesRepository extends JpaRepository<Sales, Long> {
}
