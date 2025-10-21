package com.zeebra.domain.brand.repository;

import com.zeebra.domain.brand.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
}
