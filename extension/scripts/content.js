// Content script for detecting tel: links and enhancing them
// This runs on all pages

(function () {
    'use strict';

    // Find all tel: links on the page and add visual indicator
    function enhanceTelLinks() {
        const telLinks = document.querySelectorAll('a[href^="tel:"]');

        telLinks.forEach(link => {
            // Skip if already enhanced
            if (link.dataset.clickToCallEnhanced) return;

            link.dataset.clickToCallEnhanced = 'true';

            // Add subtle indicator that this link can be sent to device
            link.title = (link.title ? link.title + ' | ' : '') + 'Right-click to call from Android device';
        });
    }

    // Run on page load
    enhanceTelLinks();

    // Watch for dynamically added links
    const observer = new MutationObserver((mutations) => {
        let shouldCheck = false;

        for (const mutation of mutations) {
            if (mutation.addedNodes.length > 0) {
                shouldCheck = true;
                break;
            }
        }

        if (shouldCheck) {
            enhanceTelLinks();
        }
    });

    observer.observe(document.body, {
        childList: true,
        subtree: true
    });
})();
