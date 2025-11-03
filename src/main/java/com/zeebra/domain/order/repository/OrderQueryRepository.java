package com.zeebra.domain.order.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zeebra.domain.order.entity.Order;
import com.zeebra.domain.order.entity.OrderStatus;
import com.zeebra.domain.order.entity.QOrder;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {
	private final JPAQueryFactory queryFactory;
	private final QOrder order = QOrder.order;



	public Page<Order> findOrdersByConditions(Long memberId, LocalDate startDate, LocalDate endDate, OrderStatus orderStatus, Pageable pageable){
		List<Order> orders = queryFactory
			.selectFrom(order)
			.where(
				memberIdEq(memberId),
				orderTimeBetween(startDate, endDate),
				orderStatusEq(orderStatus)
			)
			.orderBy(getOrderSpecifier(pageable))
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();
		Long total = queryFactory
			.select(order.count())
			.from(order)
			.where(
				memberIdEq(memberId),
				orderTimeBetween(startDate, endDate),
				orderStatusEq(orderStatus)
			)
			.fetchOne();

		return new PageImpl<>(orders, pageable, total != null ? total : 0L);
	}

	private BooleanExpression memberIdEq(Long memberId) {
		return memberId != null ? order.memberId.eq(memberId) : null;
	}

	private BooleanExpression orderTimeBetween(LocalDate startDate, LocalDate endDate) {
		if (startDate != null && endDate != null) {
			LocalDateTime startDateTime = startDate.atStartOfDay();
			LocalDateTime endDateTime = endDate.atStartOfDay();
			return order.orderTime.between(startDateTime, endDateTime);
		}
		if (startDate != null) {
			return order.orderTime.goe(startDate.atStartOfDay());
		}
		if (endDate != null) {
			return order.orderTime.lt(endDate.atStartOfDay());
		}
		return null;
	}

	private BooleanExpression orderStatusEq(OrderStatus orderStatus) {
		return orderStatus != null ? order.orderStatus.eq(orderStatus) : null;
	}

	private OrderSpecifier<?>[] getOrderSpecifier(Pageable pageable) {
		if (pageable.getSort().isEmpty()) {
			return new OrderSpecifier[]{order.orderTime.desc()};
		}

		return pageable.getSort().stream()
			.map(sortOrder -> {
				String property = sortOrder.getProperty();
				boolean isAsc = sortOrder.isAscending();

				return switch (property) {
					case "orderTime" -> isAsc ? order.orderTime.asc() : order.orderTime.desc();
					case "totalAmount" -> isAsc ? order.totalAmount.asc() : order.totalAmount.desc();
					case "orderNumber" -> isAsc ? order.orderNumber.asc() : order.orderNumber.desc();
					default -> order.orderTime.desc();
				};
			})
			.toArray(OrderSpecifier[]::new);
	}
}