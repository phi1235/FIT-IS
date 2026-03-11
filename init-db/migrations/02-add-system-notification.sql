-- Migration: Add system_notification table
-- Run manually against existing DB if upgrading from a version before this was added

CREATE TABLE IF NOT EXISTS audit.system_notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'info',
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_notif_active ON audit.system_notification(is_active, expires_at);
