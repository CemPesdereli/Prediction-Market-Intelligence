package com.example.polybets.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient polymarketWebClient(@Value("${polymarket.data-api-base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "polymarket-common-bets/0.1 (+spring-boot)")
                .build();
    }
}
