package com.legacy.customer.config;

/**
 * Anti-pattern: environment-specific values compiled into the codebase (not 12-factor).
 */
public final class LegacyAppConstants {

    public static final String DEFAULT_DB_PATH = "jdbc:h2:file:./data/customer-legacy-db;AUTO_SERVER=TRUE";
    public static final int MAX_RESULTS = 500;

    private LegacyAppConstants() {
    }
}
