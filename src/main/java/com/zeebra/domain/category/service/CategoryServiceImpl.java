package com.zeebra.domain.category.service;

import com.zeebra.domain.cart.repository.CartRepository;
import com.zeebra.domain.category.dto.CategoryRequest;
import com.zeebra.domain.category.dto.CategoryResponse;
import com.zeebra.domain.category.entity.Category;
import com.zeebra.domain.category.repository.CategoryRepository;
import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.entity.Role;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.global.ApiResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

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
            Category category = categoryRepository.save(new Category(
                    request.parentCategoryId(),
                    request.categoryName()
            ));
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
}
