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

		Long nextVal = jdbcTemplate.queryForObject(
			"SELECT COALESCE(MAX(member_id) + 1, 1) FROM members",
			Long.class
		);

		jdbcTemplate.queryForObject(
			"SELECT setval('members_member_id_seq', ?)",
			Long.class,
			nextVal
		);
	}
}