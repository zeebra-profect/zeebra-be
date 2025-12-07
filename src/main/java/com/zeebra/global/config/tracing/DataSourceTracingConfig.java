package com.zeebra.global.config.tracing;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class DataSourceTracingConfig {

	private final Tracer tracer;

	@Bean
	@Primary
	@ConditionalOnBean(DataSource.class)
	public DataSource tracingDataSource(DataSource dataSource) {
		// Boot가 만든 HikariDataSource를 TracingDataSource로 감싼다
		return new TracingDataSource(dataSource, tracer);
	}
}