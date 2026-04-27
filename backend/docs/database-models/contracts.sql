-- =============================================================================
-- Contracts Service Database Schema (PostgreSQL 15+)
-- =============================================================================
-- This schema manages contract lifecycle, state transitions, and audit trails
-- Contracts progress through states: SIGNED -> ACTIVE -> (SUSPENDED) -> (COMPLETED|CANCELED)
-- References supplier UUIDs (not foreign keys) for data isolation between services
-- =============================================================================

-- Drop existing objects (for clean deployment in development)
DROP TABLE IF EXISTS contract_state_transitions CASCADE;
DROP TABLE IF EXISTS contract_items CASCADE;
DROP TABLE IF EXISTS contracts CASCADE;

-- =============================================================================
-- CONTRACTS TABLE
-- =============================================================================
-- Main contracts table with state management and financial information
-- supplier_id is a UUID reference (no foreign key constraint)
-- Dual timestamp: creation and update timestamps for audit purposes
-- =============================================================================

CREATE TABLE contracts (
    contract_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_number VARCHAR(50) NOT NULL UNIQUE,
    supplier_id UUID NOT NULL, -- External reference (not FK), validated at application level
    
    -- Contract scope and description
    description VARCHAR(1000) NOT NULL,
    notes VARCHAR(500),
    
    -- Contract duration and lifecycle
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    
    -- Financial details
    total_amount NUMERIC(15, 2) NOT NULL CHECK (total_amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    payment_terms VARCHAR(255),
    
    -- State management
    status VARCHAR(20) NOT NULL DEFAULT 'SIGNED' CHECK (status IN ('SIGNED', 'ACTIVE', 'SUSPENDED', 'COMPLETED', 'CANCELED')),
    status_reason VARCHAR(500),
    
    -- Audit and lifecycle fields
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints and validation
    CONSTRAINT contract_number_not_empty CHECK (LENGTH(TRIM(contract_number)) > 0),
    CONSTRAINT end_date_after_start_date CHECK (end_date >= start_date),
    CONSTRAINT description_not_empty CHECK (LENGTH(TRIM(description)) > 0)
);

-- Indexes for performance (response time < 2ms requirement)
CREATE INDEX idx_contracts_contract_number ON contracts(contract_number);
CREATE INDEX idx_contracts_supplier_id ON contracts(supplier_id);
CREATE INDEX idx_contracts_status ON contracts(status);
CREATE INDEX idx_contracts_created_at ON contracts(created_at DESC);
CREATE INDEX idx_contracts_start_date ON contracts(start_date);
CREATE INDEX idx_contracts_end_date ON contracts(end_date);

-- =============================================================================
-- CONTRACT STATE TRANSITIONS TABLE
-- =============================================================================
-- Immutable audit log of all contract state transitions
-- Documents: previous_state, new_state, reason, timestamp, and who made the change
-- =============================================================================

CREATE TABLE contract_state_transitions (
    transition_id BIGSERIAL PRIMARY KEY,
    contract_id UUID NOT NULL REFERENCES contracts(contract_id) ON DELETE CASCADE,
    from_status VARCHAR(20) CHECK (from_status IN ('SIGNED', 'ACTIVE', 'SUSPENDED', 'COMPLETED', 'CANCELED')),
    to_status VARCHAR(20) NOT NULL CHECK (to_status IN ('SIGNED', 'ACTIVE', 'SUSPENDED', 'COMPLETED', 'CANCELED')),
    transition_reason VARCHAR(500),
    transitioned_by UUID NOT NULL,
    transitioned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approval_by UUID
);

-- Immutable: only INSERT allowed, never UPDATE/DELETE
-- Indexes for audit queries
CREATE INDEX idx_transitions_contract_id ON contract_state_transitions(contract_id);
CREATE INDEX idx_transitions_transitioned_at ON contract_state_transitions(transitioned_at DESC);
CREATE INDEX idx_transitions_from_status ON contract_state_transitions(from_status);
CREATE INDEX idx_transitions_to_status ON contract_state_transitions(to_status);

-- =============================================================================
-- CONTRACT ITEMS/LINE ITEMS TABLE (Optional)
-- =============================================================================
-- Detailed line items if contract contains multiple deliverables
-- Each item can have its own quantity, unit price, and status
-- =============================================================================

CREATE TABLE contract_items (
    item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contracts(contract_id) ON DELETE CASCADE,
    item_number INTEGER NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity NUMERIC(12, 2) NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(15, 2) NOT NULL CHECK (unit_price >= 0),
    unit VARCHAR(50), -- e.g., 'UNIT', 'KG', 'HOUR', 'SERVICE'
    total_price NUMERIC(15, 2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
    delivery_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT item_number_positive CHECK (item_number > 0),
    CONSTRAINT description_not_empty CHECK (LENGTH(TRIM(description)) > 0)
);

-- Indexes for performance
CREATE INDEX idx_items_contract_id ON contract_items(contract_id);
CREATE INDEX idx_items_item_number ON contract_items(item_number);

-- =============================================================================
-- TRIGGER: Update contracts.updated_at on modification
-- =============================================================================

CREATE OR REPLACE FUNCTION update_contracts_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_contracts_timestamp
BEFORE UPDATE ON contracts
FOR EACH ROW
EXECUTE PROCEDURE update_contracts_timestamp();

-- =============================================================================
-- FUNCTION: Validate contract state transitions
-- =============================================================================
-- Business rule: SIGNED -> ACTIVE -> (SUSPENDED or COMPLETED or CANCELED)
-- SIGNED contracts cannot go directly to COMPLETED or CANCELED
-- =============================================================================

CREATE OR REPLACE FUNCTION validate_contract_transition(
    p_current_status VARCHAR,
    p_new_status VARCHAR
) RETURNS BOOLEAN AS $$
BEGIN
    -- Valid transitions based on current status
    CASE p_current_status
        WHEN 'SIGNED' THEN
            -- From SIGNED: can go to ACTIVE only
            RETURN p_new_status = 'ACTIVE';
        WHEN 'ACTIVE' THEN
            -- From ACTIVE: can go to SUSPENDED, COMPLETED, or CANCELED
            RETURN p_new_status IN ('SUSPENDED', 'COMPLETED', 'CANCELED');
        WHEN 'SUSPENDED' THEN
            -- From SUSPENDED: can go back to ACTIVE, or to COMPLETED/CANCELED
            RETURN p_new_status IN ('ACTIVE', 'COMPLETED', 'CANCELED');
        WHEN 'COMPLETED' THEN
            -- COMPLETED is terminal state
            RETURN FALSE;
        WHEN 'CANCELED' THEN
            -- CANCELED is terminal state
            RETURN FALSE;
        ELSE
            RETURN FALSE;
    END CASE;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- =============================================================================
-- CONSTRAINT: Enforce valid state transitions on INSERT into state_transitions
-- =============================================================================

CREATE OR REPLACE FUNCTION check_valid_transition()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT validate_contract_transition(NEW.from_status, NEW.to_status) THEN
        RAISE EXCEPTION 'Invalid contract state transition from % to %', NEW.from_status, NEW.to_status;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_validate_transition
BEFORE INSERT ON contract_state_transitions
FOR EACH ROW
EXECUTE PROCEDURE check_valid_transition();

-- =============================================================================
-- Sample Data (for development and testing)
-- =============================================================================

INSERT INTO contracts (
    contract_number, supplier_id, description, start_date, end_date,
    total_amount, currency, status, created_by
) VALUES (
    'CTR-2026-001',
    '550e8400-e29b-41d4-a716-446655440000'::UUID, -- Example supplier UUID
    'Software development services for contract management system',
    '2026-01-15',
    '2026-06-15',
    50000.00,
    'USD',
    'SIGNED',
    '550e8400-e29b-41d4-a716-446655440001'::UUID -- Example admin user UUID
);

-- =============================================================================
-- Database Metadata and Documentation
-- =============================================================================
-- Database: db_contracts
-- Service: Contracts Microservice (Spring Boot)
-- Port: 8082
-- Authentication: JWT (Bearer tokens)
-- Timezone: UTC (all TIMESTAMPTZ fields)
-- Consistency Model: ACID (strong consistency within single service)
-- State Model: SIGNED -> ACTIVE -> (SUSPENDED | COMPLETED | CANCELED)
-- Data Isolation: supplier_id references external service (via REST API validation)
-- Partitioning: None (Sprint 1, local development)
-- =============================================================================
