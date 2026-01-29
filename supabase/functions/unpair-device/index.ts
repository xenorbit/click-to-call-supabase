// Supabase Edge Function: unpair-device
// Called by Chrome extension to disconnect/unpair a device

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

        // Create Supabase client
        const supabase = createClient(
            Deno.env.get('SUPABASE_URL') ?? '',
            Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
        )

        // Find and unpair the device
        const { data: device, error: findError } = await supabase
            .from('devices')
            .select('id')
            .eq('pairing_code', pairingCode)
            .single()

        if (findError || !device) {
            // Device not found - might already be unpaired, that's okay
            return new Response(
                JSON.stringify({ success: true, message: 'Device already disconnected' }),
                { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            )
        }

        // Mark device as unpaired
        const { error: updateError } = await supabase
            .from('devices')
            .update({
                is_paired: false,
                paired_at: null
            })
            .eq('id', device.id)

        if (updateError) {
            console.error('Update error:', updateError)
            throw updateError
        }

        console.log(`Device unpaired: ${device.id}`)

        return new Response(
            JSON.stringify({
                success: true,
                message: 'Device disconnected successfully'
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
