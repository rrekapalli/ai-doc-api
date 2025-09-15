package com.hidoc.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseHealthChecker {
    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthChecker.class);

    private final DataSource dataSource;

    public DatabaseHealthChecker(DataSource dataSource) {
        this.dataSource = dataSource;
        validateConnection();
    }

    private void validateConnection() {
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(3);
            if (valid) {
                log.info("Database connection validation succeeded");
            } else {
                log.warn("Database connection validation failed");
            }
        } catch (SQLException e) {
            log.error("Database connection validation error: {}", e.getMessage());
        }
    }
}
