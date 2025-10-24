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

    @Transactional
    @Override
    public SalesResponse createSales(Long memberId, SalesRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new NoSuchElementException("해당하는 사용자가 없습니다."));

        ProductOption productOption = productOptionRepository.findById(request.productOptionId()).orElseThrow(
                () -> new NoSuchElementException("해당하는 상품 옵션이 없습니다."));
        Sales sales = salesRepository.save(new Sales(
                productOption.getId(),
                member.getId(),
                request.price(),
                request.price(),
                request.stock(),
                SalesStatus.ON_SALE));
        return new SalesResponse(
                sales.getId(),
                sales.getProductOptionId(),
                sales.getPrice(),
                sales.getSalesStatus(),
                sales.getStock(),
                sales.getCreatedTime());
    }
}
