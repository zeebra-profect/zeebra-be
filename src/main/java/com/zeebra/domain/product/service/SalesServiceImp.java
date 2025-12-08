package com.zeebra.domain.product.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.zeebra.domain.product.entity.Product;
import com.zeebra.domain.product.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zeebra.domain.member.entity.Member;
import com.zeebra.domain.member.repository.MemberRepository;
import com.zeebra.domain.order.dto.OrderItemLine;
import com.zeebra.domain.order.dto.OrderItemResponse;
import com.zeebra.domain.product.dto.OrderSalesItem;
import com.zeebra.domain.product.dto.SalesDetailResponse;
import com.zeebra.domain.product.dto.SalesListResponse;
import com.zeebra.domain.product.dto.SalesRequest;
import com.zeebra.domain.product.dto.SalesResponse;
import com.zeebra.domain.product.dto.UserSalesItem;
import com.zeebra.domain.product.entity.ProductOption;
import com.zeebra.domain.product.entity.Sales;
import com.zeebra.domain.product.entity.SalesStatus;
import com.zeebra.global.ApiResponse;
import com.zeebra.global.ErrorCode.CommonErrorCode;
import com.zeebra.global.ErrorCode.OrderErrorCode;
import com.zeebra.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class SalesServiceImp implements SalesService {

    private final SalesRepository salesRepository;
    private final MemberRepository memberRepository;
    private final ProductOptionRepository productOptionRepository;
    private final SalesQueryRepository salesQueryRepository;
    private final ProductQueryRepository productQueryRepository;
    private final ProductRepository productRepository;

    private Sales toSales(ProductOption productOption, Member member, SalesRequest request) {
        return new Sales(
                productOption.getId(),
                member.getId(),
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
    public SalesResponse createSales(Long memberId, SalesRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new NoSuchElementException("해당하는 사용자가 없습니다."));

        ProductOption productOption = productOptionRepository.findById(request.productOptionId()).orElseThrow(
                () -> new NoSuchElementException("해당하는 상품 옵션이 없습니다."));

        Product product = productRepository.findById(productOption.getProductId()).orElseThrow(
                () -> new NoSuchElementException("해당하는 상품이 없습니다."));

        Sales sales = salesRepository.save(toSales(productOption, member, request));

        if (request.price().compareTo(productQueryRepository.lowPriceOfProduct(productOption.getProductId())) < 0) {
            product.updateMinPrice(request.price());
        }

        SalesResponse salesResponse = toSalesResponse(sales);

        return salesResponse;
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteSales(Long memberId, Long salesId) {
        try {
            Member member = memberRepository.findById(memberId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 사용자가 없습니다."));

            Sales sales = salesRepository.findById(salesId).orElseThrow(
                    () -> new NoSuchElementException("해당하는 판매 상품이 없습니다."));

            salesRepository.delete(sales);
            return ApiResponse.successMessage("판매 상품 삭제에 성공했습니다.");
        } catch (NoSuchElementException e) {
            return ApiResponse.error(null, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(null, "판매 상품을 삭제하는 과정에서 오류가 발생했습니다.");
        }
    }


    public OrderSalesItem findCheapestSalesByProductOptionId(Long productOptionId) {
        Sales sales = salesQueryRepository.findCheapestAndOldestSales(productOptionId);

        if (sales == null) {
            log.error("[최저가 판매 조회 실패] 판매 중인 상품을 찾을 수 없습니다. productOptionId: {}", productOptionId);
            throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND, "판매 중인 상품을 찾을 수 없습니다.");
        }

		return OrderSalesItem.of(sales,1);
	}

    public SalesDetailResponse getSalesDetail(Long memberId, Long salesId) {
        SalesDetailResponse salesDetailResponse = salesQueryRepository.findSalesDetailById(salesId, memberId);
        if (salesDetailResponse == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "판매 중인 상품을 찾을 수 없습니다.");
        }

        return salesDetailResponse;
    }

    @Override
    public SalesListResponse getSalesList(Long memberId, LocalDate startDate, LocalDate endDate,
                                          SalesStatus salesStatus, Pageable pageable) {
        if (memberId == null) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
        }

        LocalDate adjustedEndDate = endDate != null ? endDate.plusDays(1) : null;

        Page<SalesDetailResponse> salesResponsePage = salesQueryRepository.findSalesDetailsByConditions(
                memberId,
                startDate,
                adjustedEndDate,
                salesStatus,
                pageable
        );

        return SalesListResponse.of(salesResponsePage);
    }

    // 유저가 판매하는 상품 목록 조회
    @Override
    public List<UserSalesItem> findSalesByMemberId(Long memberId) {
        List<Sales> salesList = salesRepository.findByMemberIdAndSalesStatus(memberId, SalesStatus.ON_SALE);

        return salesList.stream()
                .map(UserSalesItem::from)
                .collect(Collectors.toList());
    }

	public void updateSalesStock(Long salesId, Integer quantity) {}

	public void updateSalesPrice(Long salesId, BigDecimal price) {}

	public void updateSalesStatus(Long salesId, SalesStatus salesStatus) {}

	public void updateSalesSoldPrice(Long salesId, BigDecimal soldPrice) {}

	public void updateSalesSoldAt(Long salesId, LocalDateTime soldAt) {}

	public List<OrderSalesItem> selectCheapestValidSales(Map<Long, Integer> productOptionQuantityMap) {
		List<Sales> salesList = salesQueryRepository.findCheapestAndOldestSales(productOptionQuantityMap);
		Map<Sales, Integer> salesQuantityMap = new HashMap<>();
		for (Map.Entry<Long, Integer> entry : productOptionQuantityMap.entrySet()) {
			Long productOptionId = entry.getKey();
			int quantity = entry.getValue();

			List<Sales> salesForOption = salesList.stream()
				.filter(sales -> sales.getProductOptionId().equals(productOptionId))
				.collect(Collectors.toList());

			for (Sales sales : salesForOption) {
				if(quantity <= 0) break;

				int quantityForOption = Math.min(quantity, sales.getStock());

				salesQuantityMap.put(sales, quantityForOption);
				quantity -= quantityForOption;
			}
		}
		List<OrderSalesItem> orderSalesItems = OrderSalesItem.of(salesQuantityMap);
		validateSalesAvailability(productOptionQuantityMap, orderSalesItems);
		return orderSalesItems;
	}

	public void validateSales(Long salesId, int quantity) {
		Sales sales = salesRepository.findById(salesId).orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "해당 판매 상품을 찾을 수 없습니다."));
		if (sales.getStock() < quantity) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "남은 재고가 없습니다.");
		}
		if (sales.getSalesStatus() != SalesStatus.ON_SALE){
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "판매 중인 상품이 없습니다.");
		}
	}

	public void validatePurchasable(List<OrderItemLine> itemLines) {
		 List<Long> salesIds = itemLines.stream()
			 .map(OrderItemLine::salesId)
			 .distinct()
			 .collect(Collectors.toList());

		 List<Sales> salesList = salesRepository.findAllById(salesIds);

		 Map<Long, Sales> salesMap = salesList.stream()
			 .collect(Collectors.toMap(Sales::getId, Function.identity()));

		 for (OrderItemLine itemLine : itemLines) {
			 Sales sales = salesMap.get(itemLine.salesId());
			 if (sales == null) {
				 throw new BusinessException(OrderErrorCode.PRODUCT_NOT_FOUND);
			 }
			 sales.validatePurchasable(itemLine.quantity());
		 }
	}

	@Transactional
	public void reserveSales(List<OrderItemResponse> orderItems) {
		orderItems.forEach(item -> {
				Sales sales = salesRepository.findById(item.saleId()).orElseThrow();
				sales.updateSalesStatus(SalesStatus.PENDING);
			});
	}

	@Transactional
	public void cancelSales(List<OrderItemResponse> orderItems) {
		orderItems.forEach(item -> {
				Sales sales = salesRepository.findById(item.saleId()).orElseThrow();
				sales.updateSalesStatus(SalesStatus.ON_SALE);
			});
	}

	@Transactional
	public void confirmSales(List<OrderItemResponse> orderItems) {
		orderItems.forEach(item -> {
				Sales sales = salesRepository.findById(item.saleId()).orElseThrow();
				sales.updateSalesStatus(SalesStatus.CONFIRMED);
				sales.updateStock(sales.getStock() - item.orderItemQuantity());
				sales.updateSoldPrice(item.orderItemPrice());
			});
	}

	private void validateSalesAvailability(Map<Long, Integer> productOptionQuantityMap, List<OrderSalesItem> cheapestSales) {

		Map<Long, Integer> salesQuantityByOption = cheapestSales.stream()
			.collect(Collectors.toMap(
				OrderSalesItem::productOptionId,
				OrderSalesItem::quantity,
				Integer::sum)
			);

		productOptionQuantityMap.forEach((optionId, requiredQuantity) -> {
			int allocatedQuantity = salesQuantityByOption.getOrDefault(optionId, 0);

			if (allocatedQuantity < requiredQuantity) {
				throw new BusinessException(OrderErrorCode.PRODUCT_OUT_OF_STOCK, "상품 재고가 부족합니다.");
			}
		});
	}
}