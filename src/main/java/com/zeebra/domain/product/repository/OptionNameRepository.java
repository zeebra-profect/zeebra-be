package com.zeebra.domain.product.repository;

import com.zeebra.domain.product.entity.OptionName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OptionNameRepository extends JpaRepository<OptionName, Long> {
}
