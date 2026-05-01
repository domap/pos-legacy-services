package com.legacy.customer.config;

/**
 * Legacy JDBC fragment retained for documentation only. Prefer {@code SPRING_DATASOURCE_URL} / {@code app.customer.*} from env.
 */
public final class LegacyAppConstants {

    public static final String DEFAULT_DB_PATH = "jdbc:h2:file:./data/customer-legacy-db;AUTO_SERVER=TRUE";

    /** @deprecated use {@code app.customer.max-results} (see {@link CustomerModuleProperties}) */
    @Deprecated
    public static final int MAX_RESULTS = 500;

    private LegacyAppConstants() {
    }
}
