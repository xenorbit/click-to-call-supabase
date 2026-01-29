// Phone number parsing and validation utilities

/**
 * Parse and normalize a phone number from text
 * @param {string} text - Raw text that may contain a phone number
 * @returns {string} - Normalized phone number or empty string
 */
export function parsePhoneNumber(text) {
    if (!text) return '';

    // Remove all non-digit characters except + at the start
    let cleaned = text.trim();

    // Check if it starts with + and preserve it
    const hasPlus = cleaned.startsWith('+');

    // Remove all non-digit characters
    cleaned = cleaned.replace(/\D/g, '');

    // Add back the + if it was there
    if (hasPlus) {
        cleaned = '+' + cleaned;
    }

    return cleaned;
}

/**
 * Validate if a string is a valid phone number
 * Basic validation - at least 7 digits, max 15 digits (E.164 standard)
 * @param {string} phoneNumber - Normalized phone number
 * @returns {boolean}
 */
export function isValidPhoneNumber(phoneNumber) {
    if (!phoneNumber) return false;

    // Remove + for counting digits
    const digitsOnly = phoneNumber.replace(/\+/g, '');

    // Must have between 7 and 15 digits (E.164 standard)
    if (digitsOnly.length < 7 || digitsOnly.length > 15) {
        return false;
    }

    // Must only contain digits and optionally start with +
    const validPattern = /^\+?\d{7,15}$/;
    return validPattern.test(phoneNumber);
}

/**
 * Format phone number for display
 * @param {string} phoneNumber - Normalized phone number
 * @returns {string} - Formatted for display
 */
export function formatPhoneNumber(phoneNumber) {
    if (!phoneNumber) return '';

    // Simple format: +1 234 567 8901
    const digitsOnly = phoneNumber.replace(/\+/g, '');
    const hasPlus = phoneNumber.startsWith('+');

    if (digitsOnly.length === 10) {
        // US format: (XXX) XXX-XXXX
        return `${hasPlus ? '+1 ' : ''}(${digitsOnly.slice(0, 3)}) ${digitsOnly.slice(3, 6)}-${digitsOnly.slice(6)}`;
    } else if (digitsOnly.length === 11 && digitsOnly.startsWith('1')) {
        // US format with country code
        return `+1 (${digitsOnly.slice(1, 4)}) ${digitsOnly.slice(4, 7)}-${digitsOnly.slice(7)}`;
    }

    // Default: just add spaces every 3 digits
    return (hasPlus ? '+' : '') + digitsOnly.replace(/(\d{3})(?=\d)/g, '$1 ').trim();
}
