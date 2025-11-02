package com.zeebra.domain.payment.repository;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zeebra.domain.order.entity.QOrder;
import com.zeebra.domain.order.entity.QOrderItem;
import com.zeebra.domain.payment.entity.QPayment;
import com.zeebra.domain.payment.entity.QPaymentHistory;
import com.zeebra.domain.payment.entity.QPaymentTransaction;
import com.zeebra.domain.product.entity.QOptionCombination;
import com.zeebra.domain.product.entity.QOptionName;
import com.zeebra.domain.product.entity.QProduct;
import com.zeebra.domain.product.entity.QProductOption;
import com.zeebra.domain.product.entity.QSales;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PaymentQueryRepository {
	private final QPayment payment = QPayment.payment;
	private final QPaymentHistory paymentHistory = QPaymentHistory.paymentHistory;
	private final QPaymentTransaction paymentTransaction = QPaymentTransaction.paymentTransaction;
	private final QOrder order = QOrder.order;
	private final QOrderItem orderItem = QOrderItem.orderItem;
	private final QSales sales = QSales.sales;
	private final QProductOption productOption = QProductOption.productOption;
	private final QProduct product = QProduct.product;
	private final QOptionCombination optionCombination = QOptionCombination.optionCombination;
	private final QOptionName optionName = QOptionName.optionName;

	private final JPAQueryFactory queryFactory;

	public boolean isPaymentOwnedByMember(Long paymentId, Long memberId){
		if (paymentId == null || memberId == null) {
			log.error("[결제 소유권 확인 실패] paymentId 또는 memberId가 null입니다. paymentId: {}, memberId: {}",
				paymentId, memberId);
			return false;
		}

		try {
			Long count = queryFactory
				.select(payment.count())
				.from(payment)
				.join(order).on(payment.orderId.eq(order.id))
				.where(
					payment.id.eq(paymentId),
					order.memberId.eq(memberId)
				)
				.fetchOne();

			boolean isOwner = count != null && count > 0;

			log.debug("[결제 소유권 확인] paymentId: {}, memberId: {}, isOwner: {}",
				paymentId, memberId, isOwner);

			return isOwner;

		} catch (Exception e) {
			log.error("[결제 소유권 확인 실패] 데이터베이스 조회 중 오류. paymentId: {}, memberId: {}, error: {}",
				paymentId, memberId, e.getMessage(), e);
			return false;
		}
	}
}