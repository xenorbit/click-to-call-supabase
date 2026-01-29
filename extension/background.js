// Click-to-Call Extension - Background Service Worker
// Handles context menu and API communication

import { sendCallRequest } from './utils/api.js';
import { parsePhoneNumber, isValidPhoneNumber } from './utils/phone-parser.js';

// Create context menu on extension install
chrome.runtime.onInstalled.addListener(() => {
    // Context menu for selected text
    chrome.contextMenus.create({
        id: 'call-from-device-selection',
        title: 'Call from Device: "%s"',
        contexts: ['selection']
    });

    // Context menu for tel: links
    chrome.contextMenus.create({
        id: 'call-from-device-link',
        title: 'Call from Device',
        contexts: ['link'],
        targetUrlPatterns: ['tel:*']
    });

    console.log('Click-to-Call extension installed (Supabase version)');
});

// Handle context menu clicks
chrome.contextMenus.onClicked.addListener(async (info, tab) => {
    let phoneNumber = '';

    if (info.menuItemId === 'call-from-device-selection') {
        // Extract from selected text
        phoneNumber = parsePhoneNumber(info.selectionText);
    } else if (info.menuItemId === 'call-from-device-link') {
        // Extract from tel: link
        phoneNumber = info.linkUrl.replace('tel:', '');
        phoneNumber = parsePhoneNumber(phoneNumber);
    }

    if (!phoneNumber || !isValidPhoneNumber(phoneNumber)) {
        // Show error notification
        chrome.action.setBadgeText({ text: '!' });
        chrome.action.setBadgeBackgroundColor({ color: '#ef4444' });

        // Clear badge after 3 seconds
        setTimeout(() => {
            chrome.action.setBadgeText({ text: '' });
        }, 3000);

        console.error('Invalid phone number:', info.selectionText || info.linkUrl);
        return;
    }

    try {
        // Show loading state
        chrome.action.setBadgeText({ text: '...' });
        chrome.action.setBadgeBackgroundColor({ color: '#3b82f6' });

        // Send call request to Supabase backend
        const result = await sendCallRequest(phoneNumber);

        if (result.success) {
            // Show success
            chrome.action.setBadgeText({ text: '✓' });
            chrome.action.setBadgeBackgroundColor({ color: '#22c55e' });
        } else {
            // Show error
            chrome.action.setBadgeText({ text: '!' });
            chrome.action.setBadgeBackgroundColor({ color: '#ef4444' });
        }

        // Clear badge after 3 seconds
        setTimeout(() => {
            chrome.action.setBadgeText({ text: '' });
        }, 3000);

    } catch (error) {
        console.error('Failed to send call request:', error);
        chrome.action.setBadgeText({ text: '!' });
        chrome.action.setBadgeBackgroundColor({ color: '#ef4444' });

        setTimeout(() => {
            chrome.action.setBadgeText({ text: '' });
        }, 3000);
    }
});

// Listen for messages from popup
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.type === 'MANUAL_CALL') {
        sendCallRequest(message.phoneNumber)
            .then(result => sendResponse(result))
            .catch(error => sendResponse({ success: false, error: error.message }));
        return true; // Keep channel open for async response
    }
});
