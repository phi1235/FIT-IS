-- FIS Bank Database Initialization Script
-- This script will run automatically when PostgreSQL container starts for the first time

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS public;

-- Grant privileges
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;

-- Database is already created by POSTGRES_DB env variable
-- Just add initial setup here if needed

COMMENT ON DATABASE fis_bank IS 'FIS Bank Application Database';

-- You can add initial tables or data here if needed
-- Spring JPA will auto-create tables based on ddl-auto: update

-- Create users table for Custom User Storage Provider
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default admin user (password: Password123)
-- BCrypt hash generated with work factor 12
INSERT INTO users (id, username, email, password, first_name, last_name, enabled)
VALUES (
    '1', 
    'admin', 
    'admin@fis.com.vn', 
    '$2b$12$dA3ESK572AIujfGuOtox2OYDddUjiRwdJvVEfuW1hAHlmGqy1aULS', 
    'System', 
    'Admin', 
    TRUE
) ON CONFLICT (username) DO NOTHING;

-- Example: Create audit log table
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id VARCHAR(255),
    user_id VARCHAR(255),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp ON audit_log(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON audit_log(user_id);

-- Add any other initialization SQL here

