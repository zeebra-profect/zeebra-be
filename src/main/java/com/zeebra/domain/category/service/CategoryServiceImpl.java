package com.zeebra.domain.category.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zeebra.domain.category.dto.CategoryRequest;
import com.zeebra.domain.category.dto.CategoryResponse;
import com.zeebra.domain.category.entity.Category;
import com.zeebra.domain.category.repository.CategoryRepository;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.global.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public ApiResponse<CategoryResponse> createCategory(Long memberId, CategoryRequest request) {
        try {
            Member member = memberRepository.findById(memberId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 사용자가 없습니다."));
            if (!member.isAdmin()) {
                return ApiResponse.error(null, "카테고리는 관리자만 생성할 수 있습니다.");
            }
            Category category = categoryRepository.save(Category.builder().name(request.categoryName()).parentId(request.parentCategoryId()).build());
            CategoryResponse categoryResponse = new CategoryResponse(
                    category.getId(),
                    category.getParentId(),
                    category.getName());
            return ApiResponse.success(categoryResponse);
        } catch (NoSuchElementException e) {
            return ApiResponse.error(null, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(null, "카테고리를 생성하는 과정에서 오류가 발생했습니다.");
        }
    }

@Transactional(readOnly = true)
public List<CategoryResponse> selectCategories() {
    return CategoryResponse.of(categoryRepository.findAllByOrderByIdAsc());
}
}