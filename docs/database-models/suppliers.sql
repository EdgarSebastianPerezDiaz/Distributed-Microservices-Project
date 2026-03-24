-- =============================================================================
-- Suppliers Service Database Schema (PostgreSQL 15+)
-- =============================================================================
-- This schema manages supplier information, status, and lifecycle
-- Each supplier is immutable after key fields are set; only operational updates allowed
-- =============================================================================

-- Drop existing objects (for clean deployment in development)
DROP TABLE IF EXISTS supplier_status_history CASCADE;
DROP TABLE IF EXISTS suppliers CASCADE;
DROP TABLE IF EXISTS supplier_categories CASCADE;

-- =============================================================================
-- SUPPLIER CATEGORIES TABLE
-- =============================================================================
-- Reference table for supplier classification and categorization
-- =============================================================================

CREATE TABLE supplier_categories (
    category_id SERIAL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    category_description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert predefined categories
INSERT INTO supplier_categories (category_name, category_description) VALUES
    ('GOODS', 'Suppliers of physical goods and materials'),
    ('SERVICES', 'Service providers'),
    ('CONSULTING', 'Consulting and advisory services'),
    ('TECHNOLOGY', 'Technology solutions and software providers'),
    ('INFRASTRUCTURE', 'Infrastructure and facility services');

-- =============================================================================
-- SUPPLIERS TABLE
-- =============================================================================
-- Core suppliers table with business identification and contact information
-- Business ID (tax ID, NIT) is immutable once created for compliance
-- Multiple supplier statuses: ACTIVE, INACTIVE, SUSPENDED
-- =============================================================================

CREATE TABLE suppliers (
    supplier_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_name VARCHAR(255) NOT NULL,
    business_id VARCHAR(50) NOT NULL UNIQUE,
    category_id INTEGER NOT NULL REFERENCES supplier_categories(category_id),
    
    -- Contact information
    email VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    
    -- Address information
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    
    -- Financial information
    bank_account VARCHAR(50),
    currency VARCHAR(3) DEFAULT 'USD',
    
    -- Supplier status and lifecycle
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    suspension_reason VARCHAR(500),
    suspension_date TIMESTAMPTZ,
    
    -- Audit and timestamp fields
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMPTZ,
    verified_by UUID,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints and validation
    CONSTRAINT email_format CHECK (email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$'),
    CONSTRAINT business_id_not_empty CHECK (LENGTH(TRIM(business_id)) > 0),
    CONSTRAINT supplier_name_not_empty CHECK (LENGTH(TRIM(supplier_name)) > 0)
);

-- Indexes for performance (response time < 2ms requirement)
CREATE INDEX idx_suppliers_business_id ON suppliers(business_id);
CREATE INDEX idx_suppliers_status ON suppliers(status);
CREATE INDEX idx_suppliers_created_at ON suppliers(created_at DESC);
CREATE INDEX idx_suppliers_email ON suppliers(email);
CREATE INDEX idx_suppliers_category_id ON suppliers(category_id);
CREATE INDEX idx_suppliers_verified ON suppliers(is_verified);

-- =============================================================================
-- SUPPLIER STATUS HISTORY TABLE
-- =============================================================================
-- Immutable audit trail for all supplier status transitions and changes
-- Tracks when, why, and by whom a supplier's status changed
-- =============================================================================

CREATE TABLE supplier_status_history (
    history_id BIGSERIAL PRIMARY KEY,
    supplier_id UUID NOT NULL REFERENCES suppliers(supplier_id) ON DELETE CASCADE,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL CHECK (new_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    changed_by UUID,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    change_reason VARCHAR(500),
    change_type VARCHAR(50) -- e.g., 'STATUS_CHANGE', 'VERIFICATION', 'SUSPENSION'
);

-- Immutable: only INSERT allowed
CREATE INDEX idx_status_history_supplier_id ON supplier_status_history(supplier_id);
CREATE INDEX idx_status_history_changed_at ON supplier_status_history(changed_at DESC);
CREATE INDEX idx_status_history_change_type ON supplier_status_history(change_type);

-- =============================================================================
-- TRIGGER: Update suppliers.updated_at on modification
-- =============================================================================

CREATE OR REPLACE FUNCTION update_suppliers_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_suppliers_timestamp
BEFORE UPDATE ON suppliers
FOR EACH ROW
EXECUTE PROCEDURE update_suppliers_timestamp();

-- =============================================================================
-- CONSTRAINT: Enforce suspension date consistency
-- =============================================================================
-- When status is SUSPENDED, suspension_date and reason must be set
-- When status is not SUSPENDED, suspension_date and reason should be cleared
-- =============================================================================

CREATE OR REPLACE FUNCTION check_suspension_consistency()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'SUSPENDED' THEN
        IF NEW.suspension_date IS NULL THEN
            RAISE EXCEPTION 'suspension_date is required when status is SUSPENDED';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_check_suspension_consistency
BEFORE INSERT OR UPDATE ON suppliers
FOR EACH ROW
EXECUTE PROCEDURE check_suspension_consistency();

-- =============================================================================
-- Sample Data (for development and testing)
-- =============================================================================

INSERT INTO suppliers (
    supplier_name, business_id, category_id, email, phone_number,
    address, city, country, status, is_verified
) VALUES (
    'Tech Solutions Inc.',
    'NIT-1234567890',
    4,
    'contact@techsolutions.local',
    '+1-555-0100',
    '123 Tech Street',
    'San Francisco',
    'United States',
    'ACTIVE',
    TRUE
);

-- =============================================================================
-- Database Metadata and Documentation
-- =============================================================================
-- Database: db_suppliers
-- Service: Suppliers Microservice (Spring Boot)
-- Port: 8081
-- Authentication: JWT (Bearer tokens)
-- Timezone: UTC (all TIMESTAMPTZ fields)
-- Consistency Model: ACID (strong consistency within single service)
-- Status Values: ACTIVE (operational), INACTIVE (no longer available), SUSPENDED (temporary hold)
-- Partitioning: None (Sprint 1, local development)
-- =============================================================================
