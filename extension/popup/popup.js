// Popup script for Click-to-Call extension

import { pairDevice, getPairingStatus, signOut } from '../utils/api.js';
import { parsePhoneNumber, isValidPhoneNumber, formatPhoneNumber } from '../utils/phone-parser.js';

// DOM Elements
const statusCard = document.getElementById('status-card');
const statusLabel = document.getElementById('status-label');
const deviceName = document.getElementById('device-name');
const pairingSection = document.getElementById('pairing-section');
const pairingCode = document.getElementById('pairing-code');
const pairBtn = document.getElementById('pair-btn');
const pairingError = document.getElementById('pairing-error');
const quickDialSection = document.getElementById('quick-dial-section');
const phoneInput = document.getElementById('phone-input');
const dialBtn = document.getElementById('dial-btn');
const dialSuccess = document.getElementById('dial-success');
const dialError = document.getElementById('dial-error');
const signoutBtn = document.getElementById('signout-btn');

// Initialize popup
async function init() {
    const status = await getPairingStatus();
    updateUI(status);
}

// Update UI based on pairing status
function updateUI(status) {
    if (status.paired) {
        statusCard.classList.add('paired');
        statusCard.classList.remove('unpaired');
        statusLabel.textContent = 'Connected';
        deviceName.textContent = status.deviceName || 'Android Device';

        pairingSection.classList.add('hidden');
        quickDialSection.classList.remove('hidden');
        signoutBtn.classList.remove('hidden');
    } else {
        statusCard.classList.remove('paired');
        statusCard.classList.add('unpaired');
        statusLabel.textContent = 'Not Connected';
        deviceName.textContent = 'No device paired';

        pairingSection.classList.remove('hidden');
        quickDialSection.classList.add('hidden');
        signoutBtn.classList.add('hidden');
    }
}

// Pair with device
async function handlePairing() {
    const code = pairingCode.value.trim();

    if (code.length !== 6 || !/^\d+$/.test(code)) {
        pairingError.textContent = 'Please enter a valid 6-digit code';
        return;
    }

    pairingError.textContent = '';
    pairBtn.disabled = true;
    pairBtn.textContent = 'Pairing...';

    try {
        const result = await pairDevice(code);

        if (result.success) {
            // Store device name
            await chrome.storage.local.set({ deviceName: result.deviceName });

            updateUI({
                paired: true,
                deviceName: result.deviceName
            });

            pairingCode.value = '';
        } else {
            pairingError.textContent = result.error || 'Pairing failed. Please try again.';
        }
    } catch (error) {
        pairingError.textContent = 'An error occurred. Please try again.';
        console.error('Pairing error:', error);
    } finally {
        pairBtn.disabled = false;
        pairBtn.textContent = 'Pair';
    }
}

// Send manual dial request
async function handleDial() {
    const rawNumber = phoneInput.value.trim();
    const phoneNumber = parsePhoneNumber(rawNumber);

    if (!isValidPhoneNumber(phoneNumber)) {
        dialError.textContent = 'Please enter a valid phone number';
        dialSuccess.textContent = '';
        return;
    }

    dialError.textContent = '';
    dialSuccess.textContent = '';
    dialBtn.disabled = true;

    try {
        // Send message to background script
        const result = await chrome.runtime.sendMessage({
            type: 'MANUAL_CALL',
            phoneNumber
        });

        if (result.success) {
            dialSuccess.textContent = `Sent ${formatPhoneNumber(phoneNumber)} to your phone!`;
            phoneInput.value = '';

            // Clear success message after 3 seconds
            setTimeout(() => {
                dialSuccess.textContent = '';
            }, 3000);
        } else {
            dialError.textContent = result.error || 'Failed to send. Please try again.';
        }
    } catch (error) {
        dialError.textContent = 'An error occurred. Please try again.';
        console.error('Dial error:', error);
    } finally {
        dialBtn.disabled = false;
    }
}

// Sign out
async function handleSignOut() {
    await signOut();
    updateUI({ paired: false });
}

// Event Listeners
pairBtn.addEventListener('click', handlePairing);
dialBtn.addEventListener('click', handleDial);
signoutBtn.addEventListener('click', handleSignOut);

// Submit on Enter key
pairingCode.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') handlePairing();
});

phoneInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') handleDial();
});

// Auto-format pairing code (numbers only)
pairingCode.addEventListener('input', (e) => {
    e.target.value = e.target.value.replace(/\D/g, '').slice(0, 6);
});

// Initialize on load
init();
