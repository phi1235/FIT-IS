-- Migration: Add SLA deadline column and seed priority data

ALTER TABLE ticket.ticket ADD COLUMN IF NOT EXISTS sla_deadline TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_ticket_sla ON ticket.ticket(sla_deadline) WHERE sla_deadline IS NOT NULL;

INSERT INTO ticket.priority (code, name, description, sla_duration_hours)
VALUES
  ('P1', 'Critical',  'Immediate attention required — system down or critical impact', 2),
  ('P2', 'High',      'Significant impact, urgent resolution needed',                 8),
  ('P3', 'Medium',    'Moderate impact, standard processing',                         24),
  ('P4', 'Low',       'Minor impact, resolve when possible',                          72)
ON CONFLICT (code) DO NOTHING;
