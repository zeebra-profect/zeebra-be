package com.zeebra.domain.sales.repository;

import com.zeebra.domain.sales.entity.Sales;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesRepository extends JpaRepository<Sales, Long> {
}
