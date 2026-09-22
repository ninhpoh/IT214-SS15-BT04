package com.example.orchestrator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Cấu hình RestClient để gọi REST API tới Payment Service và Concert Service.
 * Mỗi RestClient có base URL và timeout riêng.
 */
@Configuration
public class RestClientConfig {

    @Value("${service.payment.url}")
    private String paymentServiceUrl;

    @Value("${service.concert.url}")
    private String concertServiceUrl;

    /**
     * RestClient dùng để gọi Payment Service (port 8081).
     * Timeout 3 giây để hỗ trợ test retry khi payment timeout.
     */
    @Bean
    public RestClient paymentRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder()
                .baseUrl(paymentServiceUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * RestClient dùng để gọi Concert Service (port 8082).
     */
    @Bean
    public RestClient concertRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder()
                .baseUrl(concertServiceUrl)
                .requestFactory(factory)
                .build();
    }
}
