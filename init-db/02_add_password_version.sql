-- Migration: Add password_version column for client-side hashing migration
-- Version 1: BCrypt(plain_password) - OLD format
-- Version 2: BCrypt(SHA256(password)) - NEW format

ALTER TABLE users ADD COLUMN IF NOT EXISTS password_version INTEGER DEFAULT 1;

-- Create index for faster lookup during migration
CREATE INDEX IF NOT EXISTS idx_users_password_version ON users(password_version);

-- Comment: All existing users start with version 1 (old format)
-- When they migrate their password, it will be updated to version 2
