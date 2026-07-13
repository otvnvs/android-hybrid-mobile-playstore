package com.example.app.services;

public class WebScripts {
    public static final String INTERCEPT_SCRIPT = 
        "(function() {\n" +
        "    if (window.HasAndroidFetchIntercepted) return;\n" +
        "    window.HasAndroidFetchIntercepted = true;\n" +
        "    \n" +
        "    const originalFetch = window.fetch;\n" +
        "    window.fetch = function(input, init) {\n" +
        "        let targetUrl = '';\n" +
        "        let requestConfig = init || {};\n" +
        "        \n" +
        "        if (typeof input === 'string') {\n" +
        "            targetUrl = input;\n" +
        "        } else if (input instanceof URL) {\n" +
        "            targetUrl = input.href;\n" +
        "        } else if (input && typeof input.url === 'string') {\n" +
        "            targetUrl = input.url;\n" +
        "            if (!init) { requestConfig = input; }\n" +
        "        }\n" +
        "        \n" +
        "        const method = (requestConfig.method || 'GET').toUpperCase();\n" +
        "        const validMutations = ['POST', 'PUT', 'PATCH', 'DELETE'];\n" +
        "        \n" +
        "        if (requestConfig.body && validMutations.includes(method)) {\n" +
        "            // Telemetry Point 1: Calculate serialization cost inside JavaScript\n" +
        "            const startSerialization = performance.now();\n" +
        "            let payloadString = typeof requestConfig.body === 'string' \n" +
        "                ? requestConfig.body \n" +
        "                : JSON.stringify(requestConfig.body);\n" +
        "            const endSerialization = performance.now();\n" +
        "            \n" +
        "            if (window.AndroidBridge && typeof window.AndroidBridge.captureRequestBody === 'function') {\n" +
        "                const jsTimestampStr = String(Date.now());\n" +
        "                \n" +
        "                // Inject serialization stats into console logcat implicitly\n" +
        "                console.log('[TELEMETRY_JS] Route: ' + method + ':' + targetUrl + ' | Stringify Duration: ' + (endSerialization - startSerialization).toFixed(4) + 'ms');\n" +
        "                \n" +
        "                window.AndroidBridge.captureRequestBody(method, targetUrl, payloadString, jsTimestampStr);\n" +
        "            }\n" +
        "        }\n" +
        "        return originalFetch.apply(this, arguments);\n" +
        "    };\n" +
        "    \n" +
        "    const originalOpen = XMLHttpRequest.prototype.open;\n" +
        "    XMLHttpRequest.prototype.open = function(method, url) {\n" +
        "        this._method = method ? method.toUpperCase() : 'GET';\n" +
        "        this._url = url;\n" +
        "        return originalOpen.apply(this, arguments);\n" +
        "    };\n" +
        "    \n" +
        "    const originalSend = XMLHttpRequest.prototype.send;\n" +
        "    XMLHttpRequest.prototype.send = function(body) {\n" +
        "        const validMutations = ['POST', 'PUT', 'PATCH', 'DELETE'];\n" +
        "        if (body && validMutations.includes(this._method) && typeof this._url === 'string') {\n" +
        "            const startSerialization = performance.now();\n" +
        "            let payloadString = typeof body === 'string' ? body : JSON.stringify(body);\n" +
        "            const endSerialization = performance.now();\n" +
        "            \n" +
        "            if (window.AndroidBridge && typeof window.AndroidBridge.captureRequestBody === 'function') {\n" +
        "                console.log('[TELEMETRY_JS_AJAX] Route: ' + this._method + ':' + this._url + ' | Stringify Duration: ' + (endSerialization - startSerialization).toFixed(4) + 'ms');\n" +
        "                window.AndroidBridge.captureRequestBody(this._method, this._url, payloadString, String(Date.now()));\n" +
        "            }\n" +
        "        }\n" +
        "        return originalSend.apply(this, arguments);\n" +
        "    };\n" +
        "})();";
    // SCRIPT A: Evaluated ONCE at boot to set up polyfill definitions safely
    public static final String WEBSOCKET_PROXY_SCRIPT = "(function() {\n" +
            "    if (window.NativeSocketRegistry) return;\n" +
            "    \n" +
            "    const OriginalBrowserWebSocket = window.WebSocket;\n" +
            "    const socketInstances = new Map();\n" +
            "    \n" +
            "    window.NativeSocketRegistry = {\n" +
            "        triggerMessage: function(socketId, payload) {\n" +
            "            const clientSocket = socketInstances.get(socketId);\n" +
            "            if (clientSocket && typeof clientSocket.onmessage === 'function') {\n" +
            "                clientSocket.onmessage({ data: payload, target: clientSocket });\n" +
            "            }\n" +
            "        }\n" +
            "    };\n" +
            "    \n" +
            "    // The new constructor wrapper\n" +
            "    window.WebSocket = function(url, protocols) {\n" +
            "        const isLocalFakeRoute = url.includes('virtual-local-bridge') || url.startsWith('ws://decabase.com') || url.includes('/api/ws/');\n" +
            "        \n" +
            "        if (!isLocalFakeRoute) {\n" +
            "            console.log('[WS_ROUTER] Routing to Standard Browser Web-Stack: ' + url);\n" +
            "            return new OriginalBrowserWebSocket(url, protocols);\n" +
            "        }\n" +
            "        \n" +
            "        console.log('[WS_ROUTER] Intercepting to Native JVM Architecture: ' + url);\n" +
            "        \n" +
            "        // Expose standard browser API state properties natively\n" +
            "        this.url = url;\n" +
            "        this.readyState = 0; // CONNECTING\n" +
            "        this.extensions = '';\n" +
            "        this.protocol = '';\n" +
            "        this.binaryType = 'blob';\n" +
            "        \n" +
            "        // Set up callback hooks explicitly to avoid signature tracking misses\n" +
            "        this.onopen = null;\n" +
            "        this.onmessage = null;\n" +
            "        this.onerror = null;\n" +
            "        this.onclose = null;\n" +
            "        \n" +
            "        this._id = Math.random().toString(36).substring(2, 11) + String(Date.now());\n" +
            "        \n" +
            "        const parser = document.createElement('a');\n" +
            "        parser.href = url;\n" +
            "        const routePath = parser.pathname;\n" +
            "        \n" +
            "        socketInstances.set(this._id, this);\n" +
            "        \n" +
            "        if (window.AndroidWebSocketBridge) {\n" +
            "            window.AndroidWebSocketBridge.connectNative(this._id, routePath);\n" +
            "        }\n" +
            "        \n" +
            "        setTimeout(() => {\n" +
            "            this.readyState = 1; // OPEN\n" +
            "            if (typeof this.onopen === 'function') this.onopen();\n" +
            "        }, 15);\n" +
            "        \n" +
            "        this.send = function(data) {\n" +
            "            if (this.readyState !== 1) {\n" +
            "                throw new Error('InvalidStateError: The connection is not in an OPEN state.');\n" +
            "            }\n" +
            "            if (window.AndroidWebSocketBridge) {\n" +
            "                let serialized = typeof data === 'string' ? data : JSON.stringify(data);\n" +
            "                window.AndroidWebSocketBridge.sendNative(this._id, serialized);\n" +
            "            }\n" +
            "        };\n" +
            "        \n" +
            "        this.close = function() {\n" +
            "            if (this.readyState === 3) return;\n" +
            "            this.readyState = 3; // CLOSED\n" +
            "            if (window.AndroidWebSocketBridge) {\n" +
            "                window.AndroidWebSocketBridge.closeNative(this._id);\n" +
            "            }\n" +
            "            socketInstances.delete(this._id);\n" +
            "            if (typeof this.onclose === 'function') this.onclose();\n" +
            "        };\n" +
            "    };\n" +
            "    \n" +
            "    // Completely isolate the prototype chain from the native C++ browser bindings\n" +
            "    window.WebSocket.prototype = Object.create(Object.prototype);\n" +
            "    window.WebSocket.prototype.constructor = window.WebSocket;\n" +
            "})();";

    // SCRIPT B: Used ONLY for triggering targeted inbound text transmission frames
    public static final String WEBSOCKET_MESSAGE_FRAME_SCRIPT = 
            "if(window.NativeSocketRegistry) window.NativeSocketRegistry.triggerMessage('%s', '%s');";

}
