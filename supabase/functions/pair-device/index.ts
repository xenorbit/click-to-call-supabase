// Supabase Edge Function: pair-device
// Called by Chrome extension to validate pairing code and link device

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
        const { pairingCode } = await req.json()

        // Validate required field
        if (!pairingCode) {
            return new Response(
                JSON.stringify({ error: 'pairingCode is required' }),
                { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            )
        }

        // Validate pairing code format
        if (!/^\d{6}$/.test(pairingCode)) {
            return new Response(
                JSON.stringify({ error: 'Invalid pairing code format' }),
                { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            )
        }

        // Create Supabase client
        const supabase = createClient(
            Deno.env.get('SUPABASE_URL') ?? '',
            Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
        )

        // Find device by pairing code
        const { data: device, error } = await supabase
            .from('devices')
            .select('id, device_name, fcm_token, is_paired')
            .eq('pairing_code', pairingCode)
            .single()

        if (error || !device) {
            return new Response(
                JSON.stringify({ error: 'Invalid pairing code. No device found.' }),
                { status: 404, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            )
        }

        // Check if device has FCM token registered
        if (!device.fcm_token) {
            return new Response(
                JSON.stringify({ error: 'Device not yet registered. Please open the app on your phone first.' }),
                { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            )
        }

        // Mark device as paired
        const { error: updateError } = await supabase
            .from('devices')
            .update({
                is_paired: true,
                paired_at: new Date().toISOString()
            })
            .eq('id', device.id)

        if (updateError) {
            console.error('Update error:', updateError)
            throw updateError
        }

        console.log(`Device paired: ${device.id}`)

        return new Response(
            JSON.stringify({
                success: true,
                deviceId: device.id,
                deviceName: device.device_name,
                message: 'Device paired successfully'
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
