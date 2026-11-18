package com.bhartiyasaas.stayfile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${stayfile.http.client.connect-timeout-ms:10000}")
    private int connectTimeoutMs;

    @Value("${stayfile.http.client.read-timeout-ms:30000}")
    private int readTimeoutMs;

    @Bean
    public SimpleClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return factory;
    }

    @Bean
    public RestClient.Builder restClientBuilder(SimpleClientHttpRequestFactory clientHttpRequestFactory) {
        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory);
    }

    @Bean
    public RestClient restClient(RestClient.Builder restClientBuilder) {
        return restClientBuilder.build();
    }
}
