package com.zeebra.global.config.tracing;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestTracingFilter extends OncePerRequestFilter {

	private final Tracer tracer; // io.opentelemetry.api.trace.Tracer

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {

		String path = request.getRequestURI();
		// 주문 API에만 적용하고 싶으면 조건 걸어도 됨
		if (!path.startsWith("/api/orders")) {
			filterChain.doFilter(request, response);
			return;
		}

		Span span = tracer.spanBuilder("http.request.total")
			.setSpanKind(SpanKind.SERVER)
			.setAttribute("http.method", request.getMethod())
			.setAttribute("http.target", request.getRequestURI())
			.startSpan();

		long start = System.nanoTime();

		try (Scope scope = span.makeCurrent()) {
			filterChain.doFilter(request, response);
		} catch (Exception ex) {
			span.recordException(ex);
			span.setStatus(StatusCode.ERROR);
			throw ex;
		} finally {
			long end = System.nanoTime();
			long durationMs = (end - start) / 1_000_000;
			span.setAttribute("http.request.total_ms", durationMs);
			span.setAttribute("http.status_code", response.getStatus());
			span.end();
		}
	}
}