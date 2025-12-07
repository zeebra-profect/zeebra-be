package com.zeebra.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

@Configuration
public class OpenTelemetryConfig {

	@Bean
	public OpenTelemetry openTelemetry() {
		// OTLP Exporter (Jaeger)
		OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder()
			.setEndpoint("http://localhost:4317")   // docker면 http://jaeger:4317
			.build();

		// Span Processor
		SpanProcessor processor = BatchSpanProcessor.builder(exporter).build();

		// Tracer Provider
		SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
			.addSpanProcessor(processor)
			.setResource(Resource.getDefault().toBuilder()
				.put("service.name", "zeebra-be")
				.build())
			.build();

		// OpenTelemetry SDK
		return OpenTelemetrySdk.builder()
			.setTracerProvider(tracerProvider)
			.build();
	}

	@Bean
	public Tracer tracer(OpenTelemetry openTelemetry) {
		return openTelemetry.getTracer("zeebra-be");
	}
}