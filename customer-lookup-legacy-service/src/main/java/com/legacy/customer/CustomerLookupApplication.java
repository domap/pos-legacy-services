package com.legacy.customer;

import com.legacy.customer.config.CustomerModuleProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CustomerModuleProperties.class)
public class CustomerLookupApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerLookupApplication.class, args);
    }
}
