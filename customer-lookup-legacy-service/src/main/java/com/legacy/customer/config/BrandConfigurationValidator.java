package com.legacy.customer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Ensures at least one brand is enabled; fails startup if configuration is unusable.
 */
@Component
public class BrandConfigurationValidator {

    private static final Logger log = LoggerFactory.getLogger(BrandConfigurationValidator.class);

    private final CustomerModuleProperties properties;

    public BrandConfigurationValidator(CustomerModuleProperties properties) {
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateBrands() {
        if (properties.getBrands().isEmpty()) {
            throw new IllegalStateException("app.customer.brands must define at least one brand (see docs/MULTI_BRAND_ARCHITECTURE.md)");
        }
        long enabled = properties.getBrands().values().stream().filter(CustomerModuleProperties.BrandConfig::isEnabled).count();
        if (enabled == 0) {
            throw new IllegalStateException("At least one app.customer.brands.*.enabled must be true");
        }
        log.info("Brand configuration validated: {} brand(s) defined, {} enabled", properties.getBrands().size(), enabled);
    }
}
