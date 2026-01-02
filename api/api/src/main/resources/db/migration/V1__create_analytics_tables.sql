-- Analytics Tables for Civic Connect
-- Run this SQL against your PostgreSQL database

-- 1. App Users (registered from mobile app)
CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    google_id VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    photo_url TEXT,
    device_id VARCHAR(255),
    fcm_token TEXT,
    app_version VARCHAR(50),
    platform VARCHAR(20) DEFAULT 'ANDROID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- 2. User Sessions (tracks active sessions with heartbeat)
CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    session_token VARCHAR(255) UNIQUE NOT NULL,
    device_id VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    app_version VARCHAR(50),
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_heartbeat_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    end_reason VARCHAR(50), -- 'LOGOUT', 'TIMEOUT', 'APP_KILLED', 'FORCE_LOGOUT'
    is_active BOOLEAN DEFAULT TRUE
);

-- 3. Activity Logs (user actions in the app)
CREATE TABLE IF NOT EXISTS activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES app_users(id) ON DELETE CASCADE,
    session_id BIGINT REFERENCES user_sessions(id) ON DELETE SET NULL,
    activity_type VARCHAR(50) NOT NULL, -- 'SCREEN_VIEW', 'BUTTON_CLICK', 'API_CALL', 'LOCATION_CHANGE', 'APP_FOREGROUND', 'APP_BACKGROUND'
    activity_name VARCHAR(255) NOT NULL, -- Screen name or action identifier
    activity_data JSONB, -- Additional metadata
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Network Logs (API request/response capture)
CREATE TABLE IF NOT EXISTS network_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES app_users(id) ON DELETE CASCADE,
    session_id BIGINT REFERENCES user_sessions(id) ON DELETE SET NULL,
    request_id VARCHAR(36), -- UUID for correlation
    method VARCHAR(10) NOT NULL,
    url TEXT NOT NULL,
    request_headers JSONB,
    request_body JSONB,
    response_status INTEGER,
    response_headers JSONB,
    response_body JSONB,
    latency_ms INTEGER,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_app_users_email ON app_users(email);
CREATE INDEX IF NOT EXISTS idx_app_users_google_id ON app_users(google_id);

CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_active ON user_sessions(is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_user_sessions_token ON user_sessions(session_token);

CREATE INDEX IF NOT EXISTS idx_activity_logs_user_id ON activity_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_activity_logs_session_id ON activity_logs(session_id);
CREATE INDEX IF NOT EXISTS idx_activity_logs_created_at ON activity_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_activity_logs_type ON activity_logs(activity_type);

CREATE INDEX IF NOT EXISTS idx_network_logs_user_id ON network_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_network_logs_session_id ON network_logs(session_id);
CREATE INDEX IF NOT EXISTS idx_network_logs_created_at ON network_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_network_logs_request_id ON network_logs(request_id);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for app_users updated_at
DROP TRIGGER IF EXISTS update_app_users_updated_at ON app_users;
CREATE TRIGGER update_app_users_updated_at
    BEFORE UPDATE ON app_users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Cleanup function for old logs (run daily via cron/scheduler)
CREATE OR REPLACE FUNCTION cleanup_old_analytics_logs()
RETURNS void AS $$
BEGIN
    -- Delete network logs older than 7 days
    DELETE FROM network_logs WHERE created_at < NOW() - INTERVAL '7 days';

    -- Delete activity logs older than 30 days
    DELETE FROM activity_logs WHERE created_at < NOW() - INTERVAL '30 days';

    -- End stale sessions (no heartbeat for 5 minutes)
    UPDATE user_sessions
    SET is_active = FALSE,
        ended_at = last_heartbeat_at,
        end_reason = 'TIMEOUT'
    WHERE is_active = TRUE
    AND last_heartbeat_at < NOW() - INTERVAL '5 minutes';
END;
$$ LANGUAGE plpgsql;
