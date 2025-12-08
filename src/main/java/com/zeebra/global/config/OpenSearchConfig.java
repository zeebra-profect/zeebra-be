package com.zeebra.global.config;

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

    @Value("${opensearch.uris}")
    private String uri;

    @Value("${opensearch.username}")
    private String username;

    @Value("${opensearch.password}")
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

        OpenSearchTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()
        );

        return new OpenSearchClient(transport);
    }
}