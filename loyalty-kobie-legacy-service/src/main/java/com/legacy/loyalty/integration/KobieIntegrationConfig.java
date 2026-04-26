package com.legacy.loyalty.integration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Legacy: no timeouts tuned for prod, basic {@link RestTemplate} bean only.
 */
@Configuration
public class KobieIntegrationConfig {

    @Bean
    public RestTemplate kobieRestTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(5000);
        return new RestTemplate(f);
    }
}
