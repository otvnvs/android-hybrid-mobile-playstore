const DUMMY_DELIVERY_DATA = {
  "value": [
    {
      "ID": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "deliveryNumber": "4500176856",
      "storageLocation_id": "0001",
      "sscc": "N/A",
      "deliveryReference": "None",
      "pallets": 0,
      "cartons": 0,
      "dateReceived": "2026-06-26",
      "status": "PEND",
      "items": [
        {
          "ID": "f1e2d3c4-b5a6-9z8y-7x6w-5v4u3t2r1q0p",
          "delivery_ID": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
          "articleCode": "8233324001",
          "itemNumber": "2110",
          "description": "Cap NE 970 Rifle NY Y, White/Green, OSFM",
          "recptQty": 0,
          "targetQty": 10,
          "uom": "EA",
          "vendorId": "60843778",
          "damages": false,
          "noBarcode": false,
          "invalidBarcode": false
        },
        {
          "ID": "e2d3c4b5-a69z-8y7x-6w5v-4u3t2r1q0p1a",
          "delivery_ID": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
          "articleCode": "8233325001",
          "itemNumber": "2120",
          "description": "Cap NE 970 Rifle Chic, White/Green, OSFM",
          "recptQty": 0,
          "targetQty": 5,
          "uom": "EA",
          "vendorId": "60843782",
          "damages": false,
          "noBarcode": false,
          "invalidBarcode": false
        }
      ]
    }
  ]
};

// Immediate activation hooks to prevent reload lag inside WebViews
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()));

/**
 * Global Network Traffic Interceptor
 */
self.addEventListener('fetch', (event) => {
  const requestUrl = event.request.url;

  // 1. Target check: Only intercept specific OData catalog routes
  if (requestUrl.includes('/odata/v4/catalog/')) {
    
    // 2. Read state tracker values passed from local storage hooks via custom cookie headers
    if (requestUrl.includes('getDeliveriesByNumber')) {
      console.log('[MOCK SW] Intercepted OData Shipment Request:', requestUrl);
      
      const responseInit = {
        status: 200,
        statusText: 'OK',
        headers: { 'Content-Type': 'application/json' }
      };

      event.respondWith(
        new Response(JSON.stringify(DUMMY_DELIVERY_DATA), responseInit)
      );
    }
    
    // Intercept metadata requests to pass validation checks cleanly
    else if (requestUrl.includes('$metadata')) {
      event.respondWith(
        new Response(
          `<?xml version="1.0" encoding="utf-8"?><edmx:Edmx Version="4.0" xmlns:edmx="http://oasis-open.org"><edmx:DataServices/></edmx:Edmx>`,
          { headers: { 'Content-Type': 'application/xml' } }
        )
      );
    }
    
    // Intercept action submission updates
    else if (requestUrl.includes('submitGoodsReceipt')) {
      event.respondWith(
        new Response(
          JSON.stringify({ value: "Success: Action mock processed successfully inside service worker proxy." }),
          { headers: { 'Content-Type': 'application/json' } }
        )
      );
    }
  }
});

