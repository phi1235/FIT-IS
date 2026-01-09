-- Bank-level Microservices Database Initialization Script (PostgreSQL)
-- This script runs automatically when PostgreSQL container starts for the first time.
-- NOTE: This script creates schemas (logical DB separation) for microservices.
-- For strict "database-per-service", use separate Postgres databases instead.

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ===============================
-- AUTH SCHEMA (auth-service)
-- ===============================
CREATE SCHEMA IF NOT EXISTS auth;

-- Users (credentials)
CREATE TABLE IF NOT EXISTS auth.auth_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP NULL,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Roles
CREATE TABLE IF NOT EXISTS auth.role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Permissions
CREATE TABLE IF NOT EXISTS auth.permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(120) UNIQUE NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    module VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User <-> Role mapping (N-N)
CREATE TABLE IF NOT EXISTS auth.user_role (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_by UUID NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES auth.auth_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES auth.role(id) ON DELETE CASCADE
);

-- Role <-> Permission mapping (N-N)
CREATE TABLE IF NOT EXISTS auth.role_permission (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    assigned_by UUID NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES auth.role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES auth.permission(id) ON DELETE CASCADE
);

-- Refresh tokens
CREATE TABLE IF NOT EXISTS auth.refresh_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    device_id VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL,
    revoked_by UUID NULL,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES auth.auth_user(id) ON DELETE CASCADE
);

-- Token blacklist (optional)
CREATE TABLE IF NOT EXISTS auth.token_blacklist (
    token_hash VARCHAR(255) PRIMARY KEY,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auth_user_username ON auth.auth_user(username);
CREATE INDEX IF NOT EXISTS idx_auth_user_email ON auth.auth_user(email);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON auth.refresh_token(user_id);
CREATE INDEX IF NOT EXISTS idx_token_blacklist_expires ON auth.token_blacklist(expires_at);

-- ===============================
-- USER SCHEMA (user-service)
-- ===============================
CREATE SCHEMA IF NOT EXISTS usr;

CREATE TABLE IF NOT EXISTS usr.user_profile (
    user_id UUID PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_active_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS usr.department (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    parent_id UUID NULL,
    manager_user_id UUID NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_department_parent FOREIGN KEY (parent_id) REFERENCES usr.department(id)
);

CREATE TABLE IF NOT EXISTS usr.branch (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    address TEXT,
    phone VARCHAR(20),
    email VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS usr.user_organization (
    user_id UUID NOT NULL,
    department_id UUID NULL,
    branch_id UUID NULL,
    position VARCHAR(100),
    join_date DATE,
    is_primary BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, department_id, branch_id),
    CONSTRAINT fk_user_org_user FOREIGN KEY (user_id) REFERENCES usr.user_profile(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_org_dept FOREIGN KEY (department_id) REFERENCES usr.department(id),
    CONSTRAINT fk_user_org_branch FOREIGN KEY (branch_id) REFERENCES usr.branch(id)
);

CREATE INDEX IF NOT EXISTS idx_user_org_dept ON usr.user_organization(department_id);
CREATE INDEX IF NOT EXISTS idx_user_org_branch ON usr.user_organization(branch_id);

-- ===============================
-- TICKET SCHEMA (ticket-service)
-- ===============================
CREATE SCHEMA IF NOT EXISTS ticket;

CREATE TABLE IF NOT EXISTS ticket.ticket_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ticket.priority (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    sla_duration_hours INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ticket.ticket (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category_id UUID NULL,
    priority_id UUID NULL,
    status VARCHAR(50) NOT NULL,
    amount NUMERIC(18,2) NULL,
    maker_user_id UUID NOT NULL,
    checker_user_id UUID NULL,
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_category FOREIGN KEY (category_id) REFERENCES ticket.ticket_category(id),
    CONSTRAINT fk_ticket_priority FOREIGN KEY (priority_id) REFERENCES ticket.priority(id)
);

CREATE INDEX IF NOT EXISTS idx_ticket_status ON ticket.ticket(status);
CREATE INDEX IF NOT EXISTS idx_ticket_created_at ON ticket.ticket(created_at);
CREATE INDEX IF NOT EXISTS idx_ticket_maker ON ticket.ticket(maker_user_id);

CREATE TABLE IF NOT EXISTS ticket.ticket_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    changed_by UUID NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_hist_ticket FOREIGN KEY (ticket_id) REFERENCES ticket.ticket(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ticket_hist_ticket_time ON ticket.ticket_status_history(ticket_id, created_at);

CREATE TABLE IF NOT EXISTS ticket.ticket_comment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL,
    user_id UUID NOT NULL,
    content TEXT NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_comment_ticket FOREIGN KEY (ticket_id) REFERENCES ticket.ticket(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ticket_comment_ticket ON ticket.ticket_comment(ticket_id);

CREATE TABLE IF NOT EXISTS ticket.attachment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(100),
    storage_path TEXT NOT NULL,
    uploaded_by UUID NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_attachment_entity ON ticket.attachment(entity_type, entity_id);

-- ===============================
-- WORKFLOW SCHEMA (workflow-service / Camunda)
-- ===============================
CREATE SCHEMA IF NOT EXISTS workflow;

CREATE TABLE IF NOT EXISTS workflow.approval_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_type VARCHAR(100) NOT NULL,
    business_key VARCHAR(120),
    reference_id VARCHAR(120),
    status VARCHAR(50) NOT NULL,
    initiator_user_id UUID NOT NULL,
    current_step INT NOT NULL DEFAULT 1,
    total_steps INT NOT NULL DEFAULT 1,
    payload JSONB,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_approval_request_status ON workflow.approval_request(status);
CREATE INDEX IF NOT EXISTS idx_approval_request_type ON workflow.approval_request(request_type);
CREATE INDEX IF NOT EXISTS idx_approval_request_initiator ON workflow.approval_request(initiator_user_id);

CREATE TABLE IF NOT EXISTS workflow.approval_step (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL,
    step_number INT NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    approver_type VARCHAR(50) NOT NULL,
    approver_role_code VARCHAR(50),
    approver_user_id UUID,
    status VARCHAR(50) NOT NULL,
    comments TEXT,
    action_by UUID,
    action_at TIMESTAMP,
    due_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_approval_step_request FOREIGN KEY (request_id) REFERENCES workflow.approval_request(id) ON DELETE CASCADE,
    CONSTRAINT uq_approval_step UNIQUE (request_id, step_number)
);

CREATE INDEX IF NOT EXISTS idx_approval_step_request ON workflow.approval_step(request_id);
CREATE INDEX IF NOT EXISTS idx_approval_step_status ON workflow.approval_step(status);

CREATE TABLE IF NOT EXISTS workflow.approval_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL,
    step_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by UUID NOT NULL,
    comments TEXT,
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    CONSTRAINT fk_approval_hist_request FOREIGN KEY (request_id) REFERENCES workflow.approval_request(id) ON DELETE CASCADE,
    CONSTRAINT fk_approval_hist_step FOREIGN KEY (step_id) REFERENCES workflow.approval_step(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_approval_history_request ON workflow.approval_history(request_id);

CREATE TABLE IF NOT EXISTS workflow.camunda_process (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_definition_key VARCHAR(100) NOT NULL,
    process_instance_id VARCHAR(100) NOT NULL,
    business_key VARCHAR(120),
    status VARCHAR(50) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    variables JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_camunda_process_business_key ON workflow.camunda_process(business_key);
CREATE INDEX IF NOT EXISTS idx_camunda_process_status ON workflow.camunda_process(status);

-- ===============================
-- AUDIT SCHEMA (audit-service)
-- ===============================
CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE IF NOT EXISTS audit.audit_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id UUID,
    username VARCHAR(100),
    source_ip VARCHAR(45),
    user_agent TEXT,
    http_method VARCHAR(10),
    request_url TEXT,
    status_code INT,
    error_message TEXT,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_event_type ON audit.audit_event(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_event_time ON audit.audit_event(event_time);
CREATE INDEX IF NOT EXISTS idx_audit_event_user ON audit.audit_event(user_id);

-- ===============================
-- Seed minimal system roles/permissions (optional)
-- ===============================
INSERT INTO auth.role (code, name, description, is_system)
VALUES
  ('ADMIN', 'Administrator', 'System administrator', TRUE),
  ('MAKER', 'Maker', 'Creates requests/tickets', TRUE),
  ('CHECKER', 'Checker', 'Approves/rejects requests', TRUE),
  ('LEADER', 'Leader', 'Escalation/override approver', TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO auth.permission (code, name, module)
VALUES
  ('TICKET_CREATE', 'Create ticket', 'TICKET'),
  ('TICKET_SUBMIT', 'Submit ticket', 'TICKET'),
  ('TICKET_APPROVE', 'Approve ticket', 'TICKET'),
  ('TICKET_REJECT', 'Reject ticket', 'TICKET'),
  ('USER_VIEW', 'View users', 'USER'),
  ('USER_ROLE_ASSIGN', 'Assign roles to user', 'USER'),
  ('REPORT_EXPORT', 'Export reports', 'REPORT')
ON CONFLICT (code) DO NOTHING;
