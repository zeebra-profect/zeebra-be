package com.zeebra.domain.member.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberIdSequenceSynchronizer {

	private final JdbcTemplate jdbcTemplate;

	@PostConstruct
	public void syncMemberIdSequence() {
		String sequenceName = jdbcTemplate.queryForObject(
			"SELECT pg_get_serial_sequence('members', 'member_id')",
			String.class
		);

		Long maxId = jdbcTemplate.queryForObject(
			"SELECT COALESCE(MAX(member_id), 0) FROM members",
			Long.class
		);

		jdbcTemplate.queryForObject(
			"SELECT setval(?::regclass, ?)",
			Long.class,
			sequenceName,
			maxId
		);
	}
}