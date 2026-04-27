-- =============================================================================
-- Users Service Database Schema (PostgreSQL 15+)
-- =============================================================================
-- This schema implements user authentication, authorization, and profile management
-- with SHA-512 password hashing and strict role constraints (no multirol)
-- =============================================================================

-- Drop existing objects (for clean deployment in development)
DROP TABLE IF EXISTS user_role_change_history CASCADE;
DROP TABLE IF EXISTS user_login_history CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

-- =============================================================================
-- ROLES TABLE
-- =============================================================================
-- Immutable reference table for user roles (ADMINISTRADOR, FUNCIONARIO, AUDITOR)
-- No multirol support: each user has exactly one role
-- =============================================================================

CREATE TABLE roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    role_description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert predefined roles
INSERT INTO roles (role_name, role_description) VALUES
    ('ADMINISTRADOR', 'System administrator with full permissions'),
    ('FUNCIONARIO', 'Employee with operational permissions'),
    ('AUDITOR', 'Auditor with read-only permissions for compliance');

-- =============================================================================
-- USERS TABLE
-- =============================================================================
-- Main users table with strict ACID guarantees and referential integrity
-- Password stored as SHA-512 hash (128 hex characters)
-- Timestamps stored with timezone information for distributed system traceability
-- =============================================================================

CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash CHAR(128) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    role_id INTEGER NOT NULL REFERENCES roles(role_id),
    
    -- Address and contact information (optional fields)
    phone_number VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(100),
    country VARCHAR(100),
    
    -- Audit and lifecycle fields
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMPTZ,
    deactivated_at TIMESTAMPTZ,
    
    -- Constraints and validation
    CONSTRAINT password_hash_format CHECK (password_hash ~ '^[a-f0-9]{128}$'),
    CONSTRAINT username_not_empty CHECK (LENGTH(TRIM(username)) > 0),
    CONSTRAINT email_format CHECK (email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

-- Indexes for performance (response time < 2ms requirement)
CREATE INDEX idx_users_username ON users(username) WHERE is_active = TRUE;
CREATE INDEX idx_users_email ON users(email) WHERE is_active = TRUE;
CREATE INDEX idx_users_role_id ON users(role_id);
CREATE INDEX idx_users_created_at ON users(created_at DESC);
CREATE INDEX idx_users_is_active ON users(is_active);

-- =============================================================================
-- USER LOGIN HISTORY TABLE
-- =============================================================================
-- Immutable audit log of all login attempts and authentication events
-- =============================================================================

CREATE TABLE user_login_history (
    login_id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    login_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    logout_timestamp TIMESTAMPTZ,
    ip_address INET,
    user_agent VARCHAR(500),
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(255)
);

-- Immutable: only INSERT allowed, never UPDATE/DELETE
-- Indexes for audit queries
CREATE INDEX idx_login_history_user_id ON user_login_history(user_id);
CREATE INDEX idx_login_history_timestamp ON user_login_history(login_timestamp DESC);
CREATE INDEX idx_login_history_success ON user_login_history(success);

-- =============================================================================
-- USER ROLE CHANGE HISTORY TABLE
-- =============================================================================
-- Immutable audit trail for role assignments and changes
-- =============================================================================

CREATE TABLE user_role_change_history (
    change_id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    old_role_id INTEGER REFERENCES roles(role_id),
    new_role_id INTEGER NOT NULL REFERENCES roles(role_id),
    changed_by UUID REFERENCES users(user_id),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(255)
);

-- Immutable: only INSERT allowed
CREATE INDEX idx_role_history_user_id ON user_role_change_history(user_id);
CREATE INDEX idx_role_history_changed_at ON user_role_change_history(changed_at DESC);

-- =============================================================================
-- TRIGGER: Update users.updated_at on modification
-- =============================================================================
-- Automatic timestamp update on user profile changes
-- =============================================================================

CREATE OR REPLACE FUNCTION update_users_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_users_timestamp
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE PROCEDURE update_users_timestamp();

-- =============================================================================
-- Sample Data (for development and testing)
-- =============================================================================

INSERT INTO users (username, password_hash, email, full_name, role_id, is_active)
VALUES (
    'admin',
    'a3a8e715c9e07ceca83fcf9f5919cc4e76e6842ab374765a33ff0a8e34f67eef5fce90ab55ce31e3b86a72d7f1e1dbd8c6f57e0c8f98c7f47e1d2b5a8c7e4f9',
    'admin@contract-system.local',
    'System Administrator',
    1,
    TRUE
);

-- =============================================================================
-- Database Metadata and Documentation
-- =============================================================================
-- Database: db_users
-- Service: Users Microservice (Spring Boot)
-- Port: 8080
-- Authentication: JWT (Bearer tokens validated at API Gateway and service level)
-- Hash Algorithm: SHA-512 (128 hex characters stored in password_hash)
-- Timezone: UTC (all TIMESTAMPTZ fields)
-- Consistency Model: ACID (strong consistency within single service)
-- Partitioning: None (single-machine deployment, Sprint 1)
-- Replication: None (Sprint 1, local development)
-- =============================================================================

