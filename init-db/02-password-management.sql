-- ===============================
-- Password Management System
-- Database Migration Script
-- Created: 2026-01-09
-- ===============================

-- ===============================
-- 1. Password Reset Token Table
-- ===============================
CREATE TABLE IF NOT EXISTS auth.password_reset_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    reset_code VARCHAR(10) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    token_type VARCHAR(20) NOT NULL,
    
    -- Verification state
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_consumed BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP NULL,
    consumed_at TIMESTAMP NULL,
    
    -- Security fields
    ip_address VARCHAR(45),
    user_agent TEXT,
    failed_attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    
    -- Expiration & audit
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NULL,
    
    CONSTRAINT fk_pwd_reset_user FOREIGN KEY (user_id) REFERENCES auth.auth_user(id) ON DELETE CASCADE,
    CONSTRAINT check_token_type CHECK (token_type IN ('FORGOT_PASSWORD', 'CHANGE_PASSWORD', 'ADMIN_RESET'))
);

CREATE INDEX idx_password_reset_token_user_id ON auth.password_reset_token(user_id);
CREATE INDEX idx_password_reset_token_hash ON auth.password_reset_token(token_hash);
CREATE INDEX idx_password_reset_expires ON auth.password_reset_token(expires_at);
CREATE INDEX idx_password_reset_verified ON auth.password_reset_token(is_verified, is_consumed);
CREATE INDEX idx_password_reset_created ON auth.password_reset_token(created_at DESC);

-- ===============================
-- 2. Email Template Table
-- ===============================
CREATE TABLE IF NOT EXISTS auth.email_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_code VARCHAR(100) UNIQUE NOT NULL,
    template_name VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    html_body TEXT NOT NULL,
    text_body TEXT,
    
    -- Template variables as JSON array
    required_variables TEXT,
    
    -- Versioning & audit
    version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL,
    updated_by UUID NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_email_template_creator FOREIGN KEY (created_by) REFERENCES auth.auth_user(id),
    CONSTRAINT fk_email_template_updater FOREIGN KEY (updated_by) REFERENCES auth.auth_user(id)
);

CREATE INDEX idx_email_template_code ON auth.email_template(template_code);
CREATE INDEX idx_email_template_active ON auth.email_template(is_active);
CREATE INDEX idx_email_template_created ON auth.email_template(created_at DESC);

-- ===============================
-- 3. Email Audit Log Table
-- ===============================
CREATE TABLE IF NOT EXISTS auth.email_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    template_code VARCHAR(100) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    
    subject VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    status_reason TEXT,
    
    ip_address VARCHAR(45),
    user_agent TEXT,
    reset_token_id UUID,
    
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT fk_email_audit_user FOREIGN KEY (user_id) REFERENCES auth.auth_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_email_audit_reset_token FOREIGN KEY (reset_token_id) REFERENCES auth.password_reset_token(id) ON DELETE SET NULL,
    CONSTRAINT check_email_status CHECK (status IN ('SENT', 'FAILED', 'BOUNCE', 'COMPLAINT'))
);

CREATE INDEX idx_email_audit_user ON auth.email_audit_log(user_id);
CREATE INDEX idx_email_audit_status ON auth.email_audit_log(status);
CREATE INDEX idx_email_audit_template ON auth.email_audit_log(template_code);
CREATE INDEX idx_email_audit_sent ON auth.email_audit_log(sent_at DESC);

-- ===============================
-- 4. Update auth_user Table
-- ===============================
ALTER TABLE auth.auth_user ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE auth.auth_user ADD COLUMN IF NOT EXISTS password_expire_at TIMESTAMP;
ALTER TABLE auth.auth_user ADD COLUMN IF NOT EXISTS force_password_change BOOLEAN DEFAULT FALSE;
ALTER TABLE auth.auth_user ADD COLUMN IF NOT EXISTS password_attempts INT DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_auth_user_password_expire ON auth.auth_user(password_expire_at);
CREATE INDEX IF NOT EXISTS idx_auth_user_force_pwd_change ON auth.auth_user(force_password_change);

-- ===============================
-- 5. Insert Default Email Templates
-- ===============================

-- Get admin user ID (assuming first user is admin)
DO $$
DECLARE
    admin_id UUID;
BEGIN
    SELECT id INTO admin_id FROM auth.auth_user LIMIT 1;
    
    IF admin_id IS NOT NULL THEN
        -- Password Reset Template
        INSERT INTO auth.email_template (
            template_code, template_name, subject, html_body, text_body,
            required_variables, version, is_active, created_by, updated_by
        ) VALUES (
            'PASSWORD_RESET',
            'Password Reset Request',
            'Password Reset Code - FIS Bank',
            '<html><body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 20px; border-radius: 8px;">
                    <h2 style="color: #0066cc;">Password Reset Request</h2>
                    <p>Hi [[userName]],</p>
                    <p>We received a request to reset your password for FIS Bank.</p>
                    <div style="background-color: #e8f0f8; padding: 15px; border-left: 4px solid #0066cc; margin: 20px 0;">
                        <p><strong>Your Reset Code:</strong></p>
                        <h3 style="letter-spacing: 5px; color: #0066cc;">[[resetCode]]</h3>
                        <p style="color: #666; font-size: 14px;">This code will expire in [[expiryMinutes]] minutes.</p>
                    </div>
                    <p style="color: #999; font-size: 14px;">
                        <strong>Security Tips:</strong>
                        <ul>
                            <li>Never share this code with anyone</li>
                            <li>FIS Bank staff will never ask for this code</li>
                            <li>If you did not request this, please ignore this email</li>
                        </ul>
                    </p>
                    <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
                    <p style="font-size: 12px; color: #999;">
                        For security issues, contact: [[supportEmail]]<br>
                        © [[currentYear]] FIS Bank. All rights reserved.
                    </p>
                </div>
            </body></html>',
            'Hi [[userName]], Your reset code is: [[resetCode]] (expires in [[expiryMinutes]] minutes). Never share this code. Contact [[supportEmail]] if needed.',
            '["userName", "resetCode", "expiryMinutes", "supportEmail", "currentYear"]',
            1, TRUE, admin_id, admin_id
        ) ON CONFLICT (template_code) DO NOTHING;

        -- Change Password Confirmation Template
        INSERT INTO auth.email_template (
            template_code, template_name, subject, html_body, text_body,
            required_variables, version, is_active, created_by, updated_by
        ) VALUES (
            'PASSWORD_CHANGED',
            'Password Changed Confirmation',
            'Your Password Has Been Changed - FIS Bank',
            '<html><body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 20px; border-radius: 8px;">
                    <h2 style="color: #0066cc;">Password Changed Successfully</h2>
                    <p>Hi [[userName]],</p>
                    <p>Your password has been changed successfully.</p>
                    <div style="background-color: #e8f8f0; padding: 15px; border-left: 4px solid #00aa66; margin: 20px 0;">
                        <p style="color: #00aa66;"><strong>✓ Change Date:</strong> [[changeDate]]</p>
                        <p style="color: #00aa66;"><strong>✓ Changed From:</strong> [[ipAddress]]</p>
                    </div>
                    <p style="color: #999; font-size: 14px;">
                        <strong>If you did not make this change:</strong>
                        <ul>
                            <li>Change your password immediately</li>
                            <li>Contact our support team</li>
                            <li>File a security incident report</li>
                        </ul>
                    </p>
                    <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
                    <p style="font-size: 12px; color: #999;">
                        Security Team: [[supportEmail]]<br>
                        © [[currentYear]] FIS Bank. All rights reserved.
                    </p>
                </div>
            </body></html>',
            'Your password was changed on [[changeDate]] from [[ipAddress]]. If not you, contact [[supportEmail]] immediately.',
            '["userName", "changeDate", "ipAddress", "supportEmail", "currentYear"]',
            1, TRUE, admin_id, admin_id
        ) ON CONFLICT (template_code) DO NOTHING;

        -- Admin Reset Password Template
        INSERT INTO auth.email_template (
            template_code, template_name, subject, html_body, text_body,
            required_variables, version, is_active, created_by, updated_by
        ) VALUES (
            'ADMIN_PASSWORD_RESET',
            'Admin Password Reset',
            'Your Password Has Been Reset by Administrator - FIS Bank',
            '<html><body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; background-color: #f9f9f9; padding: 20px; border-radius: 8px;">
                    <h2 style="color: #cc6600;">Password Reset by Administrator</h2>
                    <p>Hi [[userName]],</p>
                    <p>An administrator has reset your password. Please use the code below to set a new password.</p>
                    <div style="background-color: #fff0e6; padding: 15px; border-left: 4px solid #cc6600; margin: 20px 0;">
                        <p><strong>Your Reset Code:</strong></p>
                        <h3 style="letter-spacing: 5px; color: #cc6600;">[[resetCode]]</h3>
                        <p style="color: #666; font-size: 14px;">
                            This code will expire in [[expiryMinutes]] minutes.<br>
                            <strong>You must set a new password after this reset.</strong>
                        </p>
                    </div>
                    <p style="background-color: #ffe6e6; padding: 10px; border-radius: 4px; color: #c00;">
                        <strong>⚠ Important:</strong> If you did not request this reset, contact the administrator immediately.
                    </p>
                    <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
                    <p style="font-size: 12px; color: #999;">
                        Support: [[supportEmail]]<br>
                        © [[currentYear]] FIS Bank. All rights reserved.
                    </p>
                </div>
            </body></html>',
            'Admin reset your password. Use code [[resetCode]] to set a new one (expires in [[expiryMinutes]] minutes). Contact [[supportEmail]] if needed.',
            '["userName", "resetCode", "expiryMinutes", "supportEmail", "currentYear"]',
            1, TRUE, admin_id, admin_id
        ) ON CONFLICT (template_code) DO NOTHING;

    END IF;
END $$;

-- ===============================
-- 6. Audit Trail Table for Password Changes
-- ===============================
CREATE TABLE IF NOT EXISTS auth.password_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_password_hash_last_4_chars VARCHAR(4),
    new_password_hash_last_4_chars VARCHAR(4),
    
    initiated_by UUID,  -- NULL if user self-service, UUID if admin
    initiated_reason TEXT,
    
    ip_address VARCHAR(45),
    user_agent TEXT,
    status VARCHAR(20) NOT NULL,
    status_message TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_pwd_audit_user FOREIGN KEY (user_id) REFERENCES auth.auth_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_pwd_audit_initiated_by FOREIGN KEY (initiated_by) REFERENCES auth.auth_user(id) ON DELETE SET NULL,
    CONSTRAINT check_pwd_audit_action CHECK (action IN ('FORGOT_PASSWORD', 'CHANGE_PASSWORD', 'ADMIN_RESET', 'FORCED_CHANGE')),
    CONSTRAINT check_pwd_audit_status CHECK (status IN ('SUCCESS', 'FAILED', 'ATTEMPTED'))
);

CREATE INDEX idx_password_audit_user ON auth.password_audit(user_id);
CREATE INDEX idx_password_audit_action ON auth.password_audit(action);
CREATE INDEX idx_password_audit_created ON auth.password_audit(created_at DESC);
CREATE INDEX idx_password_audit_status ON auth.password_audit(status);

COMMIT;
