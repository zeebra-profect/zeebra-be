package com.zeebra.global.config.tracing;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TracingDataSource extends DelegatingDataSource {

	private final Tracer tracer;

	public TracingDataSource(DataSource delegate, Tracer tracer) {
		super(delegate);
		this.tracer = tracer;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return traceGetConnection("datasource.getConnection");
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return traceGetConnection("datasource.getConnection.withUser");
	}

	private Connection traceGetConnection(String spanName) throws SQLException {
		Span span = tracer.spanBuilder(spanName).startSpan();
		long start = System.nanoTime();

		try (Scope scope = span.makeCurrent()) {
			Connection conn = getTargetDataSource().getConnection();
			long durationMs = (System.nanoTime() - start) / 1_000_000;
			span.setAttribute("db.connection.wait_ms", durationMs);
			span.setAttribute("db.system", "postgresql");
			span.setAttribute("db.pool", "HikariPool-API");
			return conn;
		} catch (SQLException ex) {
			span.recordException(ex);
			span.setStatus(StatusCode.ERROR);
			throw ex;
		} finally {
			span.end();
		}
	}
}