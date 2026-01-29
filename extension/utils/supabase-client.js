// Supabase Configuration
// Replace these values with your Supabase project details

// Get these from: Supabase Dashboard > Project Settings > API
export const SUPABASE_URL = 'https://yfqagnhsiamagtewxxiz.supabase.co';
export const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlmcWFnbmhzaWFtYWd0ZXd4eGl6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njg0NTM0NTUsImV4cCI6MjA4NDAyOTQ1NX0.5s2XwssFnB_eAnkJRcvDz8wwwXj7y8lcHWPIxcON0-g';

// Edge Function endpoints (auto-generated from SUPABASE_URL)
export const ENDPOINTS = {
    registerDevice: `${SUPABASE_URL}/functions/v1/register-device`,
    pairDevice: `${SUPABASE_URL}/functions/v1/pair-device`,
    unpairDevice: `${SUPABASE_URL}/functions/v1/unpair-device`,
    sendCallRequest: `${SUPABASE_URL}/functions/v1/send-call-request`
};
