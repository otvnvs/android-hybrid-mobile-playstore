const CACHE_NAME = 'app-workspace-v1';

// Lifecycle 1: Install event - sets up static asset shells
self.addEventListener('install', function(event) {
  console.log('[SERVICE WORKER] Install event fired.');
  // Forces the waiting service worker to become the active service worker immediately
  self.skipWaiting();
});

// Lifecycle 2: Activate event - FIXED: Forces immediate operational takeover
self.addEventListener('activate', function(event) {
  console.log('[SERVICE WORKER] Activate event fired. Claiming active client frames...');
  
  // CRITICAL TAKE-OVER FIX: Enforce immediate tab claiming
  event.waitUntil(
    self.clients.claim().then(function() {
      console.log('[SERVICE WORKER] Tab clients successfully claimed natively.');
    })
  );
});

// Lifecycle 3: Network Interception Pipeline
self.addEventListener('fetch', function(event) {
  event.respondWith(fetch(event.request));
});

