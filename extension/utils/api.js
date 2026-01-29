// API utilities for communicating with Supabase Edge Functions

import { ENDPOINTS, SUPABASE_ANON_KEY } from './supabase-client.js';

// Common headers for all Supabase requests
const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${SUPABASE_ANON_KEY}`,
    'apikey': SUPABASE_ANON_KEY
});

/**
 * Get the stored pairing data from chrome.storage
 * @returns {Promise<{pairingCode: string, deviceId: string, deviceName: string}>}
 */
async function getPairingData() {
    return new Promise((resolve) => {
        chrome.storage.local.get(['pairingCode', 'deviceId', 'deviceName'], (result) => {
            resolve({
                pairingCode: result.pairingCode || '',
                deviceId: result.deviceId || '',
                deviceName: result.deviceName || ''
            });
        });
    });
}

/**
 * Send a call request to the paired Android device
 * @param {string} phoneNumber - The phone number to dial
 * @returns {Promise<{success: boolean, error?: string}>}
 */
export async function sendCallRequest(phoneNumber) {
    const { pairingCode } = await getPairingData();

    if (!pairingCode) {
        return {
            success: false,
            error: 'Not paired with any device. Please pair your Android device first.'
        };
    }

    try {
        console.log('📞 Sending call request to Supabase...', { phoneNumber, pairingCode });

        const response = await fetch(ENDPOINTS.sendCallRequest, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify({
                phoneNumber,
                pairingCode
            })
        });

        const data = await response.json();

        if (!response.ok) {
            console.error('Call request failed:', data);
            return {
                success: false,
                error: data.error || 'Failed to send call request'
            };
        }

        console.log('✅ Call request sent successfully:', data);
        return {
            success: true,
            data
        };

    } catch (error) {
        console.error('API Error:', error);
        return {
            success: false,
            error: 'Network error. Please check your connection.'
        };
    }
}

/**
 * Pair with an Android device using a pairing code
 * @param {string} pairingCode - The 6-digit code displayed on the Android app
 * @returns {Promise<{success: boolean, deviceName?: string, error?: string}>}
 */
export async function pairDevice(pairingCode) {
    try {
        console.log('🔗 Pairing with device...', { pairingCode });

        const response = await fetch(ENDPOINTS.pairDevice, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify({
                pairingCode
            })
        });

        const data = await response.json();

        if (!response.ok) {
            console.error('Pairing failed:', data);
            return {
                success: false,
                error: data.error || 'Failed to pair device. Make sure the code is correct.'
            };
        }

        // Store the pairing data in chrome.storage
        await chrome.storage.local.set({
            pairingCode: pairingCode,
            deviceId: data.deviceId,
            deviceName: data.deviceName || 'Android Device',
            pairedAt: Date.now()
        });

        console.log('✅ Paired successfully:', data);
        return {
            success: true,
            deviceName: data.deviceName || 'Android Device',
            deviceId: data.deviceId
        };

    } catch (error) {
        console.error('Pairing Error:', error);
        return {
            success: false,
            error: 'Network error. Please check your connection and try again.'
        };
    }
}

/**
 * Get the current pairing status
 * @returns {Promise<{paired: boolean, deviceName?: string}>}
 */
export async function getPairingStatus() {
    const { pairingCode, deviceName } = await getPairingData();

    if (!pairingCode) {
        return { paired: false };
    }

    return {
        paired: true,
        deviceName: deviceName || 'Android Device'
    };
}

/**
 * Disconnect and sign out - unpairs device in Supabase and clears local data
 */
export async function signOut() {
    try {
        const { pairingCode } = await getPairingData();

        // If we have a pairing code, unpair in Supabase first
        if (pairingCode) {
            console.log('🔌 Disconnecting device...', { pairingCode });

            await fetch(ENDPOINTS.unpairDevice, {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify({ pairingCode })
            });
            // We don't need to check the response - even if it fails, 
            // we still want to clear local storage
        }

        // Clear all local storage
        await chrome.storage.local.clear();
        console.log('✅ Signed out and disconnected');

        return { success: true };
    } catch (error) {
        console.error('Sign out error:', error);
        // Still clear local storage even on error
        await chrome.storage.local.clear();
        return { success: true };
    }
}
