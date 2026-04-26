package com.legacy.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Deploy as WAR to external Tomcat; also runnable via {@code main} for local dev.
 */
@SpringBootApplication
public class CustomerLookupApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(CustomerLookupApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(CustomerLookupApplication.class, args);
    }
}
