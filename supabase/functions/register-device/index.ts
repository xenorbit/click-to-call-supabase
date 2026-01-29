// Supabase Edge Function: register-device
// Called by Android app to register FCM token with pairing code

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
    // Handle CORS preflight
    if (req.method === 'OPTIONS') {
        return new Response('ok', { headers: corsHeaders })
    }

    try {
        const { pairingCode, fcmToken, deviceName } = await req.json()

        // Validate required fields
        if (!pairingCode || !fcmToken) {
            return new Response(
                JSON.stringify({ error: 'pairingCode and fcmToken are required' }),
                { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            )
        }

        // Validate pairing code format (6 digits)
        if (!/^\d{6}$/.test(pairingCode)) {
            return new Response(
                JSON.stringify({ error: 'Pairing code must be 6 digits' }),
                { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            )
        }

        // Create Supabase client with service role key
        const supabase = createClient(
            Deno.env.get('SUPABASE_URL') ?? '',
            Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
        )

        // Upsert device (update if exists, insert if not)
        const { data, error } = await supabase
            .from('devices')
            .upsert({
                pairing_code: pairingCode,
                fcm_token: fcmToken,
                device_name: deviceName || 'Android Device',
                updated_at: new Date().toISOString()
            }, {
                onConflict: 'pairing_code',
                ignoreDuplicates: false
            })
            .select()
            .single()

        if (error) {
            console.error('Database error:', error)
            throw error
        }

        console.log(`Device registered: ${data.id} with pairing code: ${pairingCode}`)

        return new Response(
            JSON.stringify({
                success: true,
                deviceId: data.id,
                message: 'Device registered successfully'
            }),
            { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        )

    } catch (error) {
        console.error('Error:', error)
        return new Response(
            JSON.stringify({ error: error.message || 'Internal server error' }),
            { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        )
    }
})
