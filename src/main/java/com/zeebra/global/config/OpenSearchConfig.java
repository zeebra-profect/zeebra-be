package com.zeebra.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.opensearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenSearchConfig {

    @Value("${spring.opensearch.uris}")
    private String uri;

    @Value("${spring.opensearch.username}")
    private String username;

    @Value("${spring.opensearch.password}")
    private String password;

    @Bean
    public OpenSearchClient openSearchClient() {
        // URI 파싱
        String cleanUri = uri.replace("https://", "").replace("http://", "");
        String scheme = uri.startsWith("https") ? "https" : "http";
        String hostName;
        int port = 443;

        if (cleanUri.contains(":")) {
            String[] parts = cleanUri.split(":");
            hostName = parts[0];
            port = Integer.parseInt(parts[1]);
        } else {
            hostName = cleanUri;
        }

        HttpHost host = new HttpHost(scheme, hostName, port);

        // 인증 설정
        final BasicCredentialsProvider credentialsProvider =
                new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(host.getHostName(), host.getPort()),
                new UsernamePasswordCredentials(username, password.toCharArray())
        );

        // RestClient 생성
        RestClient restClient = RestClient
                .builder(host)
                .setRequestConfigCallback(requestConfigBuilder ->
                        requestConfigBuilder
                                .setConnectTimeout(Timeout.ofSeconds(5))       // 연결: 5초
                                .setResponseTimeout(Timeout.ofSeconds(180))    // 응답: 3분 (p95 1분 고려)
                )
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                                .setDefaultCredentialsProvider(credentialsProvider)
                )
                .build();

        // ⭐ ObjectMapper 커스터마이징
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());  // Java 8 날짜/시간 타입 지원
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);  // ISO-8601 문자열 사용

        // ⭐ 커스텀 ObjectMapper를 JacksonJsonpMapper에 주입
        OpenSearchTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper(objectMapper)  // 여기만 변경!
        );

        return new OpenSearchClient(transport);
    }
}