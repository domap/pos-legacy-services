package com.legacy.customer.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Twelve-factor + multi-brand shape: env-driven caps and per-brand policy (datasource routing is future work).
 */
@ConfigurationProperties(prefix = "app.customer")
@Validated
public class CustomerModuleProperties {

    @Min(1)
    @Max(10_000)
    private int maxResults = 500;

    private Map<String, @Valid BrandConfig> brands = new LinkedHashMap<>();

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public Map<String, BrandConfig> getBrands() {
        return brands;
    }

    public void setBrands(Map<String, BrandConfig> brands) {
        this.brands = brands;
    }

    public static class BrandConfig {

        private boolean enabled = true;

        @NotBlank
        private String datasourceRef = "primary";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDatasourceRef() {
            return datasourceRef;
        }

        public void setDatasourceRef(String datasourceRef) {
            this.datasourceRef = datasourceRef;
        }
    }
}
