-- V1__create_tenant_tables.sql
-- This script runs once per new schema (e.g., logs, metrics, etc.)

CREATE TABLE logs (
                      id UUID PRIMARY KEY,
                      loglevel VARCHAR(100),
                      message TEXT NOT NULL,
                      timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                      metadata jsonb
);

CREATE INDEX idx_logs_timestamp ON logs (timestamp);
CREATE INDEX idx_logs_loglevel ON logs (loglevel);