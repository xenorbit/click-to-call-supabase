// Supabase Edge Function: send-call-request
// Sends push notification via FCM V1 API using service account

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { encode as base64Encode } from "https://deno.land/std@0.168.0/encoding/base64.ts"

const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

// Get OAuth2 access token from service account
async function getAccessToken(serviceAccountJson: string): Promise<string> {
    try {
        const sa = JSON.parse(serviceAccountJson)

        if (!sa.private_key || !sa.client_email || !sa.project_id) {
            throw new Error('Invalid service account JSON - missing required fields')
        }

        const now = Math.floor(Date.now() / 1000)

        // Create JWT header and payload
        const header = { alg: 'RS256', typ: 'JWT' }
        const payload = {
            iss: sa.client_email,
            scope: 'https://www.googleapis.com/auth/firebase.messaging',
            aud: 'https://oauth2.googleapis.com/token',
            iat: now,
            exp: now + 3600
        }

        // Base64URL encode function
        const base64url = (data: string) => {
            return btoa(data).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
        }

        const encodedHeader = base64url(JSON.stringify(header))
        const encodedPayload = base64url(JSON.stringify(payload))
        const unsignedToken = `${encodedHeader}.${encodedPayload}`

        // Import private key and sign
        const pemContents = sa.private_key
            .replace('-----BEGIN PRIVATE KEY-----', '')
            .replace('-----END PRIVATE KEY-----', '')
            .replace(/\n/g, '')

        const binaryKey = Uint8Array.from(atob(pemContents), c => c.charCodeAt(0))

        const cryptoKey = await crypto.subtle.importKey(
            'pkcs8',
            binaryKey.buffer,
            { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
            false,
            ['sign']
        )

        const signature = await crypto.subtle.sign(
            'RSASSA-PKCS1-v1_5',
            cryptoKey,
            new TextEncoder().encode(unsignedToken)
        )

        const encodedSignature = base64url(String.fromCharCode(...new Uint8Array(signature)))
        const jwt = `${unsignedToken}.${encodedSignature}`

        // Exchange JWT for access token
        const tokenResponse = await fetch('https://oauth2.googleapis.com/token', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`
        })

        const tokenData = await tokenResponse.json()

        if (!tokenData.access_token) {
            console.error('Token response:', tokenData)
            throw new Error('Failed to get access token: ' + (tokenData.error_description || tokenData.error || 'Unknown error'))
        }

        return tokenData.access_token
    } catch (error) {
        console.error('getAccessToken error:', error)
        throw error
    }
}

serve(async (req) => {
    // Handle CORS preflight
    if (req.method === 'OPTIONS') {
        return new Response('ok', { headers: corsHeaders })
    }

    try {
        const { phoneNumber, pairingCode } = await req.json()

        // Validate required fields
        if (!phoneNumber || !pairingCode) {
            return new Response(
                JSON.stringify({ error: 'phoneNumber and pairingCode are required' }),
                { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            )
        }

        // Create Supabase client
        const supabase = createClient(
            Deno.env.get('SUPABASE_URL') ?? '',
            Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
        )

        // Get device by pairing code
        const { data: device, error } = await supabase
            .from('devices')
            .select('id, fcm_token, is_paired')
            .eq('pairing_code', pairingCode)
            .single()

        if (error || !device) {
            console.error('Device lookup error:', error)
            return new Response(
                JSON.stringify({ error: 'Device not found. Please check pairing code.' }),
                { status: 404, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            )
        }

        if (!device.fcm_token) {
            return new Response(
                JSON.stringify({ error: 'Device FCM token not registered' }),
                { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            )
        }

        // Get service account from secrets
        const serviceAccountJson = Deno.env.get('FCM_SERVICE_ACCOUNT')

        if (!serviceAccountJson) {
            console.error('FCM_SERVICE_ACCOUNT not set')
            return new Response(
                JSON.stringify({ error: 'FCM_SERVICE_ACCOUNT secret not configured' }),
                { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            )
        }

        // Parse service account to get project ID
        let projectId: string
        try {
            const sa = JSON.parse(serviceAccountJson)
            projectId = sa.project_id
        } catch (e) {
            console.error('Failed to parse service account:', e)
            return new Response(
                JSON.stringify({ error: 'Invalid FCM_SERVICE_ACCOUNT JSON' }),
                { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            )
        }

        // Get access token
        let accessToken: string
        try {
            accessToken = await getAccessToken(serviceAccountJson)
        } catch (e) {
            console.error('Failed to get access token:', e)
            return new Response(
                JSON.stringify({ error: 'Failed to authenticate with FCM: ' + e.message }),
                { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            )
        }

        // Send FCM V1 push notification
        const fcmUrl = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`

        const fcmPayload = {
            message: {
                token: device.fcm_token,
                // ONLY send data payload - NO notification!
                // This forces Android to call onMessageReceived even when app is in background
                data: {
                    type: 'CALL_REQUEST',
                    phoneNumber: phoneNumber,
                    timestamp: Date.now().toString()
                },
                android: {
                    priority: 'high'
                }
                // DO NOT include 'notification' field - it bypasses our code when app is in background!
            }
        }

        console.log('Sending FCM to:', fcmUrl)

        const fcmResponse = await fetch(fcmUrl, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(fcmPayload)
        })

        const fcmResult = await fcmResponse.json()

        if (!fcmResponse.ok) {
            console.error('FCM error:', fcmResult)
            return new Response(
                JSON.stringify({
                    error: 'Failed to send push notification',
                    details: fcmResult.error?.message || fcmResult
                }),
                { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
            )
        }

        // Log the call request
        await supabase.from('call_logs').insert({
            device_id: device.id,
            phone_number: phoneNumber,
            status: 'sent'
        })

        console.log(`Call request sent: ${phoneNumber} to device ${device.id}`)

        return new Response(
            JSON.stringify({
                success: true,
                message: 'Call request sent to your phone'
            }),
            { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        )

    } catch (error) {
        console.error('Unhandled error:', error)
        return new Response(
            JSON.stringify({ error: error.message || 'Internal server error' }),
            { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        )
    }
})
