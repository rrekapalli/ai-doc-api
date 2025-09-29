-- Database schema for Hi-Doc API Service (Flyway removed)

-- Subscribers table
CREATE TABLE IF NOT EXISTS subscribers (
    user_id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    oauth_provider VARCHAR(50) NOT NULL,
    subscription_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    purchase_date TIMESTAMP NULL,
    app_store_platform VARCHAR(50) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Usage tracking table
CREATE TABLE IF NOT EXISTS usage_tracking (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    ai_provider VARCHAR(50) NOT NULL,
    request_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT NULL,
    month_year VARCHAR(7) NOT NULL,
    CONSTRAINT fk_usage_user FOREIGN KEY (user_id) REFERENCES subscribers(user_id)
);

CREATE INDEX IF NOT EXISTS idx_usage_user_month ON usage_tracking(user_id, month_year);
CREATE INDEX IF NOT EXISTS idx_usage_timestamp ON usage_tracking(request_timestamp);

-- Health data entries
CREATE TABLE IF NOT EXISTS health_data_entries (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL,
    value TEXT NOT NULL,
    unit VARCHAR(50) NULL,
    timestamp TIMESTAMP NOT NULL,
    notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_health_user FOREIGN KEY (user_id) REFERENCES subscribers(user_id)
);

CREATE INDEX IF NOT EXISTS idx_health_user_time ON health_data_entries(user_id, timestamp);

-- Analytics summary table
CREATE TABLE IF NOT EXISTS analytics_summary (
    id BIGSERIAL PRIMARY KEY,
    metric_type VARCHAR(100) NOT NULL,
    metric_value BIGINT NOT NULL,
    dimension_1 VARCHAR(100) NULL,
    dimension_2 VARCHAR(100) NULL,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
