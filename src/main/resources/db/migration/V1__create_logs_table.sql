-- V1__create_logs_table.sql
-- This script will be run for each new tenant schema.

CREATE TABLE logs (
                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                      log_level VARCHAR(50) NOT NULL,
                      message TEXT NOT NULL,
                      timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
                      metadata JSONB,
                      created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Optional: Add indexes for faster lookups
CREATE INDEX idx_logs_timestamp ON logs(timestamp);
CREATE INDEX idx_logs_log_level ON logs(log_level);