package com.zeebra.domain.product.repository;

import com.zeebra.domain.product.entity.OptionCombination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OptionCombinationRepository extends JpaRepository<OptionCombination, Long> {
}
