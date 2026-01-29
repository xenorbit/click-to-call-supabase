-- Supabase Database Schema for Click-to-Call
-- Run this in Supabase SQL Editor

-- Devices table (stores Android device info and FCM tokens)
CREATE TABLE devices (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  pairing_code VARCHAR(6) UNIQUE NOT NULL,
  fcm_token TEXT,
  device_name VARCHAR(100) DEFAULT 'Android Device',
  is_paired BOOLEAN DEFAULT false,
  paired_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

-- Call logs table (tracks all call requests)
CREATE TABLE call_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  device_id UUID REFERENCES devices(id) ON DELETE CASCADE,
  phone_number VARCHAR(30) NOT NULL,
  status VARCHAR(20) DEFAULT 'sent',
  created_at TIMESTAMPTZ DEFAULT now()
);

-- Index for faster pairing code lookups
CREATE INDEX idx_devices_pairing_code ON devices(pairing_code);

-- Enable Row Level Security
ALTER TABLE devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE call_logs ENABLE ROW LEVEL SECURITY;

-- Allow all operations for single-user beta (no auth restrictions)
CREATE POLICY "Allow all for beta" ON devices FOR ALL USING (true);
CREATE POLICY "Allow all for beta" ON call_logs FOR ALL USING (true);
