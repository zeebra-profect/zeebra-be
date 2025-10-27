package com.zeebra.domain.product.service;

import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.product.dto.SalesRequest;
import com.zeebra.domain.product.dto.SalesResponse;
import com.zeebra.domain.product.entity.ProductOption;
import com.zeebra.domain.product.entity.Sales;
import com.zeebra.domain.product.entity.SalesStatus;
import com.zeebra.domain.product.repository.ProductOptionRepository;
import com.zeebra.domain.product.repository.SalesRepository;
import com.zeebra.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesServiceImp implements SalesService {

    private final SalesRepository salesRepository;
    private final MemberRepository memberRepository;
    private final ProductOptionRepository productOptionRepository;

    private Sales toSales(ProductOption productOption, Member member, SalesRequest request) {
        return new Sales(
                productOption.getId(),
                member.getId(),
                request.price(),
                request.price(),
                request.stock(),
                SalesStatus.ON_SALE);
    }

    private SalesResponse toSalesResponse(Sales sales) {
        return new SalesResponse(
                sales.getId(),
                sales.getProductOptionId(),
                sales.getPrice(),
                sales.getSalesStatus(),
                sales.getStock(),
                sales.getCreatedTime());
    }

    @Transactional
    @Override
    public ApiResponse<SalesResponse> createSales(Long memberId, SalesRequest request) {
        try {
            Member member = memberRepository.findById(memberId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 사용자가 없습니다."));

            ProductOption productOption = productOptionRepository.findById(request.productOptionId()).orElseThrow(
                    () -> new NoSuchElementException("해당하는 상품 옵션이 없습니다."));
            Sales sales = salesRepository.save(toSales(productOption, member, request));
            SalesResponse salesResponse = toSalesResponse(sales);
            return ApiResponse.success(salesResponse);
        } catch (NoSuchElementException e) {
            return ApiResponse.error(null, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(null, "판매 상품을 생성하는 과정에서 오류가 발생했습니다.");
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteSales(Long memberId, Long salesId) {
        try{
            Member member = memberRepository.findById(memberId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 사용자가 없습니다."));

            Sales sales = salesRepository.findById(salesId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 판매 상품이 없습니다."));

            salesRepository.delete(sales);
            return ApiResponse.successMessage("판매 상품 삭제에 성공했습니다.");
        } catch (NoSuchElementException e){
            return ApiResponse.error(null, e.getMessage());
        } catch (Exception e){
            return ApiResponse.error(null,"판매 상품을 삭제하는 과정에서 오류가 발생했습니다.");
        }
    }
}
