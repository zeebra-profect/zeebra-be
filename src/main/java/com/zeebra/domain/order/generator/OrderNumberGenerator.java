package com.zeebra.domain.order.generator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {
	private static final String ORDER_NUMBER_DATE_FORMAT = "yyyyMMdd";
	private static final String ORDER_NUMBER_TIME_FORMAT = "HHmm";

	private final JdbcTemplate jdbcTemplate;

	public String generate(LocalDateTime now) {
		Long sequence = jdbcTemplate.queryForObject("select nextval('order_number_seq')", Long.class);

		String datePart = now.format(DateTimeFormatter.ofPattern(ORDER_NUMBER_DATE_FORMAT));
		String timePart = now.format(DateTimeFormatter.ofPattern(ORDER_NUMBER_TIME_FORMAT));
		String sequencePart = String.format("%06d", sequence);

		return String.format("%s-%s%s", datePart, timePart, sequencePart);
	}
}