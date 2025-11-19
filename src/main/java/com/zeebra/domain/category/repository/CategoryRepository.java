package com.zeebra.domain.category.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zeebra.domain.category.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
	List<Category> findAllByOrderByIdAsc();
	List<Category> findAllByOrderByParentIdAscIdAsc();
}