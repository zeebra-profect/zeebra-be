//package com.zeebra.global.config;
//
//
//import co.elastic.clients.elasticsearch.ElasticsearchClient;
//import co.elastic.clients.json.jackson.JacksonJsonpMapper;
//import co.elastic.clients.transport.rest_client.RestClientTransport;
//import org.apache.http.HttpHost;
//import org.apache.http.client.config.RequestConfig;
//import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
//import org.elasticsearch.client.RestClient;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class ElasticsearchConfig {
//
//    @Bean
//    public ElasticsearchClient elasticsearchClient() {
//        RestClient restClient = RestClient
//                .builder(new HttpHost("localhost", 9200, "http"))
//                .setHttpClientConfigCallback(this::configureHttpClient)
//                .build();
//
//        RestClientTransport transport = new RestClientTransport(
//                restClient,
//                new JacksonJsonpMapper()
//        );
//
//        return new ElasticsearchClient(transport);
//    }
//
//    private HttpAsyncClientBuilder configureHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
//        return httpClientBuilder
//                .setMaxConnTotal(100)
//                .setMaxConnPerRoute(50)
//                .setDefaultRequestConfig(
//                        RequestConfig.custom()
//                                .setConnectTimeout(5000)
//                                .setSocketTimeout(60000)
//                                .setConnectionRequestTimeout(5000)
//                                .build()
//                );
//    }
//}
