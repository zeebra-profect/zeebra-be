package com.zeebra.domain.product.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.zeebra.global.ErrorCode.SalesErrorCode;
import com.zeebra.global.exception.BusinessException;
import com.zeebra.global.jpa.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Sales extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productOptionId;

    private Long memberId;

    private BigDecimal price;

    private BigDecimal soldPrice;

    private int stock;

	@Enumerated(EnumType.ORDINAL)
    private SalesStatus salesStatus;

    private LocalDateTime soldAt;

    public Sales(Long productOptionId, Long memberId, BigDecimal price, int stock, SalesStatus salesStatus) {
        this.productOptionId = productOptionId;
        this.memberId = memberId;
        this.price = price;
        this.stock = stock;
        this.salesStatus = salesStatus;
    }

	public void validatePurchasable(int quantity) {
		if (!this.salesStatus.isOnSale()){
			throw new BusinessException(SalesErrorCode.SALES_NOT_AVAILABLE);
		}
		if (quantity <= 0) {
			throw new BusinessException(SalesErrorCode.INVALID_QUANTITY);
		}
		if (quantity > this.stock) {
			throw new BusinessException(SalesErrorCode.OUT_OF_STOCK);
		}
	}
}