package com.zeebra.domain.product.repository;

import java.util.List;
import java.util.Optional;

import com.zeebra.domain.product.entity.SalesStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import com.zeebra.domain.product.entity.Sales;

public interface SalesRepository extends JpaRepository<Sales, Long> {
	Optional<Sales> findByIdAndMemberId(Long id, Long memberId);

    List<Sales> findByMemberIdAndSalesStatus(Long memberId, SalesStatus status);
}