const http = require('http');
const https = require('https');
const url = require('url');
const fs = require('fs');
const path = require('path');
const querystring = require('querystring');
const crypto = require('crypto');

const PORT = process.env.DEFAULT_APP_PORT || 3000;
const TOKENS_FILE = path.join(__dirname, 'tokens.json');

// --- In-memory OAuth State Storage for CSRF Protection ---
// Map of state -> { createdAt: number }
const activeOAuthStates = new Map();

function generateOAuthState() {
  const now = Date.now();
  // Clean up states older than 15 minutes
  for (const [key, val] of activeOAuthStates.entries()) {
    if (now - val.createdAt > 15 * 60 * 1000) {
      activeOAuthStates.delete(key);
    }
  }
  const state = crypto.randomBytes(24).toString('hex');
  activeOAuthStates.set(state, { createdAt: now });
  return state;
}

function validateAndConsumeOAuthState(state) {
  if (!state || typeof state !== 'string') return false;
  const entry = activeOAuthStates.get(state);
  if (!entry) return false;
  activeOAuthStates.delete(state); // One-time use to prevent replay
  const now = Date.now();
  if (now - entry.createdAt > 15 * 60 * 1000) return false; // Expired
  return true;
}

// --- Helper Functions to Resolve Credentials ---
// STRICT: Only reads from server-side environment/secrets.
// Standardized ONLY on GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET.
// NEVER uses hardcoded, demo, or fallback credentials.
function getCredentials() {
  let clientId = (process.env.GOOGLE_CLIENT_ID || '').trim();
  let clientSecret = (process.env.GOOGLE_CLIENT_SECRET || '').trim();

  // Inspect platform secrets files (/app/.dev.env.json, /app/applet/.dev.env.json)
  if (!clientId || !clientSecret) {
    const jsonPaths = ['/app/.dev.env.json', '/app/applet/.dev.env.json'];
    for (const p of jsonPaths) {
      if (fs.existsSync(p)) {
        try {
          const data = JSON.parse(fs.readFileSync(p, 'utf8'));
          if (!clientId && data.GOOGLE_CLIENT_ID) clientId = String(data.GOOGLE_CLIENT_ID).trim();
          if (!clientSecret && data.GOOGLE_CLIENT_SECRET) clientSecret = String(data.GOOGLE_CLIENT_SECRET).trim();
        } catch (e) {
          console.error(`Error reading ${p}:`, e.message);
        }
      }
    }
  }

  // Inspect environment files (/app/.env, /app/applet/.env, .env)
  if (!clientId || !clientSecret) {
    const envPaths = ['/app/.env', '/app/applet/.env', path.join(__dirname, '.env')];
    for (const p of envPaths) {
      if (fs.existsSync(p)) {
        try {
          const content = fs.readFileSync(p, 'utf8');
          for (const line of content.split('\n')) {
            const trimmed = line.trim();
            if (trimmed.startsWith('#') || !trimmed.includes('=')) continue;
            const idx = trimmed.indexOf('=');
            const key = trimmed.substring(0, idx).trim();
            const val = trimmed.substring(idx + 1).trim().replace(/^["']|["']$/g, '');
            if (!clientId && key === 'GOOGLE_CLIENT_ID') clientId = val;
            if (!clientSecret && key === 'GOOGLE_CLIENT_SECRET') clientSecret = val;
          }
        } catch (e) {
          console.error(`Error reading ${p}:`, e.message);
        }
      }
    }
  }

  return { clientId, clientSecret };
}

// --- Safe Diagnostic Helper for Google OAuth Credentials ---
function getOAuthDiagnostics() {
  const { clientId, clientSecret } = getCredentials();

  const isClientIdConfigured = Boolean(clientId && clientId.length > 5);
  const isClientSecretConfigured = Boolean(clientSecret && clientSecret.length > 5);
  const isOAuthFullyConfigured = isClientIdConfigured && isClientSecretConfigured;

  // Mask client ID safely: show only first 12 chars and domain suffix
  let maskedClientId = 'Not Configured';
  if (isClientIdConfigured) {
    if (clientId.length > 25) {
      maskedClientId = clientId.substring(0, 12) + '...' + clientId.substring(clientId.lastIndexOf('-'));
    } else {
      maskedClientId = clientId.substring(0, 6) + '...';
    }
  }

  // Scan across runtime sources to detect if multiple disparate Google Client IDs exist
  const foundClientIds = new Set();
  if (process.env.GOOGLE_CLIENT_ID && process.env.GOOGLE_CLIENT_ID.trim()) {
    foundClientIds.add(process.env.GOOGLE_CLIENT_ID.trim());
  }
  if (process.env.GOOGLE_OAUTH_CLIENT_ID && process.env.GOOGLE_OAUTH_CLIENT_ID.trim()) {
    foundClientIds.add(process.env.GOOGLE_OAUTH_CLIENT_ID.trim());
  }
  if (process.env.GOOGLE_CLIENTID && process.env.GOOGLE_CLIENTID.trim()) {
    foundClientIds.add(process.env.GOOGLE_CLIENTID.trim());
  }

  const jsonPaths = ['/app/.dev.env.json', '/app/applet/.dev.env.json', path.join(__dirname, '..', '.dev.env.json')];
  for (const p of jsonPaths) {
    if (fs.existsSync(p)) {
      try {
        const data = JSON.parse(fs.readFileSync(p, 'utf8'));
        if (data.GOOGLE_CLIENT_ID && String(data.GOOGLE_CLIENT_ID).trim()) {
          foundClientIds.add(String(data.GOOGLE_CLIENT_ID).trim());
        }
        if (data.GOOGLE_OAUTH_CLIENT_ID && String(data.GOOGLE_OAUTH_CLIENT_ID).trim()) {
          foundClientIds.add(String(data.GOOGLE_OAUTH_CLIENT_ID).trim());
        }
      } catch (e) {}
    }
  }

  const multipleClientIdsDetected = foundClientIds.size > 1;
  const diagnosticMessage = multipleClientIdsDetected
    ? '⚠️ WARNING: Multiple conflicting Google Client IDs were detected in application configuration sources. Application is standardized to use ONLY GOOGLE_CLIENT_ID.'
    : '✓ Single Google Cloud Console Web Application OAuth Client ID active. No multiple or conflicting Client IDs detected.';

  let configSource = 'None';
  if (process.env.GOOGLE_CLIENT_ID && process.env.GOOGLE_CLIENT_SECRET) {
    configSource = 'Server Environment Variables (GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET)';
  } else if (isOAuthFullyConfigured) {
    configSource = 'Server Secrets Store (/app/.dev.env.json)';
  } else if (isClientIdConfigured) {
    configSource = 'Partial Server Configuration (Missing Client Secret)';
  }

  return {
    googleClientIdConfigured: isClientIdConfigured,
    googleClientSecretConfigured: isClientSecretConfigured,
    googleOAuthStatus: isOAuthFullyConfigured ? 'Configured ✓' : 'Not Configured',
    clientIdStatus: isClientIdConfigured ? 'Configured ✓' : 'Not Configured',
    clientSecretStatus: isClientSecretConfigured ? 'Configured ✓' : 'Not Configured',
    oauthCallbackStatus: 'Configured ✓',
    clientIdConfigured: isClientIdConfigured,
    clientSecretConfigured: isClientSecretConfigured,
    clientIdMasked: maskedClientId,
    multipleClientIdsDetected,
    diagnosticMessage,
    configSource
  };
}

function getBaseUrl(req) {
  if (process.env.APP_URL) {
    return process.env.APP_URL.replace(/\/$/, '');
  }
  const host = req ? (req.headers['x-forwarded-host'] || req.headers['host']) : 'localhost:8080';
  const proto = (req && req.headers['x-forwarded-proto']) ? req.headers['x-forwarded-proto'] : 'https';
  if (host.includes('run.app')) {
    return `https://${host}`;
  }
  return 'https://ais-dev-sugu7tqqkbfnyqa4ix6qc2-617494223617.asia-east1.run.app';
}

function getRedirectUri(req) {
  return `${getBaseUrl(req)}/api/auth/google/callback`;
}

const SCOPES = [
  'https://www.googleapis.com/auth/gmail.modify',
  'https://www.googleapis.com/auth/calendar',
  'https://www.googleapis.com/auth/drive.file',
  'https://www.googleapis.com/auth/userinfo.email',
  'https://www.googleapis.com/auth/userinfo.profile'
];

// --- Token Persistence ---
function loadTokens() {
  try {
    if (fs.existsSync(TOKENS_FILE)) {
      return JSON.parse(fs.readFileSync(TOKENS_FILE, 'utf8'));
    }
  } catch (e) {
    console.error('Error loading tokens:', e);
  }
  return null;
}

function saveTokens(data) {
  try {
    fs.writeFileSync(TOKENS_FILE, JSON.stringify(data, null, 2), 'utf8');
  } catch (e) {
    console.error('Error saving tokens:', e);
  }
}

function deleteTokens() {
  try {
    if (fs.existsSync(TOKENS_FILE)) {
      fs.unlinkSync(TOKENS_FILE);
    }
  } catch (e) {
    console.error('Error deleting tokens:', e);
  }
}

// --- HTTPS Request Helper ---
function makeHttpsRequest(options, postData = null) {
  return new Promise((resolve, reject) => {
    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        let parsed;
        try {
          parsed = JSON.parse(body);
        } catch (e) {
          parsed = body;
        }
        resolve({
          statusCode: res.statusCode,
          headers: res.headers,
          data: parsed
        });
      });
    });

    req.on('error', reject);
    req.setTimeout(30000, () => {
      req.destroy();
      reject(new Error('Request timed out after 30 seconds'));
    });

    if (postData) {
      req.write(postData);
    }
    req.end();
  });
}

// --- Token Refresh Helper ---
async function getValidAccessToken() {
  const tokenData = loadTokens();
  if (!tokenData || !tokenData.access_token) {
    throw new Error('NOT_CONNECTED');
  }

  // Check expiration (buffer 60 seconds)
  const now = Date.now();
  if (tokenData.expires_at && now < tokenData.expires_at - 60000) {
    return tokenData.access_token;
  }

  // Need to refresh using refresh_token
  if (!tokenData.refresh_token) {
    throw new Error('REAUTHORIZATION_REQUIRED: No refresh token available');
  }

  const { clientId, clientSecret } = getCredentials();
  const postData = querystring.stringify({
    client_id: clientId,
    client_secret: clientSecret,
    refresh_token: tokenData.refresh_token,
    grant_type: 'refresh_token'
  });

  const resp = await makeHttpsRequest({
    hostname: 'oauth2.googleapis.com',
    path: '/token',
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Content-Length': Buffer.byteLength(postData)
    }
  }, postData);

  if (resp.statusCode !== 200) {
    console.error('Token refresh failed:', resp.data);
    throw new Error('REAUTHORIZATION_REQUIRED: ' + (resp.data.error_description || resp.data.error || 'Refresh failed'));
  }

  const newTokens = resp.data;
  tokenData.access_token = newTokens.access_token;
  tokenData.expires_at = Date.now() + (newTokens.expires_in * 1000);
  if (newTokens.refresh_token) {
    tokenData.refresh_token = newTokens.refresh_token;
  }
  saveTokens(tokenData);
  return tokenData.access_token;
}

// --- Body Parsing Helper ---
function parseBody(req) {
  return new Promise((resolve) => {
    let body = '';
    req.on('data', chunk => body += chunk);
    req.on('end', () => {
      try {
        resolve(JSON.parse(body || '{}'));
      } catch (e) {
        resolve(querystring.parse(body || ''));
      }
    });
  });
}

// --- HTTP Server ---
const server = http.createServer(async (req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;
  const cleanPath = (pathname || '/').replace(/\/+$/, '') || '/';

  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  try {
    // 1. Safe Backend Diagnostic Endpoint returning strictly:
    // { "googleClientIdConfigured": true/false, "googleClientSecretConfigured": true/false }
    if ((cleanPath === '/api/oauth/diagnostic' || cleanPath === '/api/auth/google/diagnostic' || cleanPath === '/api/diagnostic') && req.method === 'GET') {
      const diag = getOAuthDiagnostics();
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        googleClientIdConfigured: diag.googleClientIdConfigured,
        googleClientSecretConfigured: diag.googleClientSecretConfigured
      }));
      return;
    }

    // 2. Comprehensive OAuth configuration endpoint for assistant UI
    if ((cleanPath === '/api/oauth/config' || cleanPath === '/api/auth/google/config') && req.method === 'GET') {
      const { clientId, clientSecret } = getCredentials();
      const redirectUri = getRedirectUri(req);
      const tokenData = loadTokens();
      const diag = getOAuthDiagnostics();

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        googleClientIdConfigured: diag.googleClientIdConfigured,
        googleClientSecretConfigured: diag.googleClientSecretConfigured,
        googleOAuth: diag.googleOAuthStatus,
        clientId: diag.clientIdStatus,
        clientSecret: diag.clientSecretStatus,
        oauthCallback: diag.oauthCallbackStatus,
        clientIdConfigured: diag.clientIdConfigured,
        clientSecretConfigured: diag.clientSecretConfigured,
        clientIdMasked: diag.maskedClientId,
        redirectUri: redirectUri,
        multipleClientIdsDetected: diag.multipleClientIdsDetected,
        diagnosticMessage: diag.diagnosticMessage,
        configSource: diag.configSource,
        gmailApiStatus: 'Ready',
        calendarApiStatus: 'Ready',
        driveApiStatus: 'Ready',
        connectionStatus: tokenData ? 'Connected' : 'Not Connected',
        connectedEmail: tokenData ? tokenData.email : null
      }));
      return;
    }

    // 2. Generate Google OAuth Authorization URL with CSRF state
    if (cleanPath === '/api/auth/google/url' && req.method === 'GET') {
      const { clientId, clientSecret } = getCredentials();
      const redirectUri = getRedirectUri(req);

      if (!clientId || !clientSecret) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          error: 'Google OAuth is not configured on the server. Please set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET environment variables/secrets.',
          oauthConfigured: false
        }));
        return;
      }

      const state = generateOAuthState();
      const authUrl = `https://accounts.google.com/o/oauth2/v2/auth?` + querystring.stringify({
        client_id: clientId,
        redirect_uri: redirectUri,
        response_type: 'code',
        scope: SCOPES.join(' '),
        access_type: 'offline',
        prompt: 'consent',
        state: state
      });

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ authUrl, redirectUri, state }));
      return;
    }

    // 3. OAuth Callback endpoint (Validates state and exchanges code for tokens)
    if (cleanPath === '/api/auth/google/callback') {
      const code = parsedUrl.query.code;
      const error = parsedUrl.query.error;
      const state = parsedUrl.query.state;

      if (error) {
        res.writeHead(400, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(`
          <!DOCTYPE html>
          <html>
          <head><title>Authorization Failed - NIAZ AI</title><meta name="viewport" content="width=device-width, initial-scale=1"></head>
          <body style="font-family:system-ui,sans-serif;background:#070C1B;color:#FFF;padding:40px;text-align:center;">
            <h2 style="color:#EF4444;">Authorization Cancelled or Failed</h2>
            <p style="color:#94A3B8;">Reason: ${error}</p>
            <p style="margin-top:20px;"><a href="/" style="color:#00E5FF;text-decoration:none;font-weight:bold;">Return to Portal</a></p>
          </body>
          </html>
        `);
        return;
      }

      // CSRF State parameter validation
      if (!validateAndConsumeOAuthState(state)) {
        res.writeHead(400, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(`
          <!DOCTYPE html>
          <html>
          <head><title>Invalid OAuth State - NIAZ AI</title><meta name="viewport" content="width=device-width, initial-scale=1"></head>
          <body style="font-family:system-ui,sans-serif;background:#070C1B;color:#FFF;padding:40px;text-align:center;">
            <h2 style="color:#EF4444;">OAuth State Validation Failed</h2>
            <p style="color:#94A3B8;">The OAuth state token was invalid or expired. Please re-initiate authorization from the assistant portal.</p>
            <p style="margin-top:20px;"><a href="/" style="color:#00E5FF;text-decoration:none;font-weight:bold;">Return to Portal</a></p>
          </body>
          </html>
        `);
        return;
      }

      if (!code) {
        res.writeHead(400, { 'Content-Type': 'text/plain' });
        res.end('Missing authorization code');
        return;
      }

      const { clientId, clientSecret } = getCredentials();
      const redirectUri = getRedirectUri(req);

      const postData = querystring.stringify({
        code,
        client_id: clientId,
        client_secret: clientSecret,
        redirect_uri: redirectUri,
        grant_type: 'authorization_code'
      });

      const tokenResp = await makeHttpsRequest({
        hostname: 'oauth2.googleapis.com',
        path: '/token',
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'Content-Length': Buffer.byteLength(postData)
        }
      }, postData);

      if (tokenResp.statusCode !== 200) {
        console.error('Token exchange error:', tokenResp.data);
        res.writeHead(500, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(`
          <!DOCTYPE html>
          <html>
          <head><title>Token Exchange Error - NIAZ AI</title></head>
          <body style="font-family:system-ui,sans-serif;background:#0A1128;color:#FFF;padding:40px;text-align:center;">
            <h2 style="color:#EF4444;">Failed to exchange authorization code</h2>
            <pre style="background:#131D38;padding:16px;text-align:left;display:inline-block;border-radius:8px;">${JSON.stringify(tokenResp.data, null, 2)}</pre>
            <p><a href="/" style="color:#00E5FF;">Return to Portal</a></p>
          </body>
          </html>
        `);
        return;
      }

      const tokenData = tokenResp.data;
      const accessToken = tokenData.access_token;
      const refreshToken = tokenData.refresh_token;
      const expiresIn = tokenData.expires_in || 3600;

      // Fetch user profile info
      let userEmail = 'Unknown';
      let userName = 'User';
      try {
        const userinfoResp = await makeHttpsRequest({
          hostname: 'www.googleapis.com',
          path: '/oauth2/v2/userinfo',
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${accessToken}`
          }
        });
        if (userinfoResp.statusCode === 200 && userinfoResp.data) {
          userEmail = userinfoResp.data.email || userEmail;
          userName = userinfoResp.data.name || userName;
        }
      } catch (e) {
        console.error('Failed to fetch userinfo:', e);
      }

      // Preserve existing refresh token if not newly supplied
      const existing = loadTokens();
      const finalRefreshToken = refreshToken || (existing ? existing.refresh_token : null);

      saveTokens({
        email: userEmail,
        name: userName,
        connected: true,
        connectedAt: new Date().toISOString(),
        access_token: accessToken,
        refresh_token: finalRefreshToken,
        expires_at: Date.now() + (expiresIn * 1000),
        scopes: SCOPES
      });

      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(`
        <!DOCTYPE html>
        <html>
        <head>
          <title>Google Account Connected - NIAZ AI ASSISTANT</title>
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <style>
            body { background: #070C1B; color: #FFF; font-family: system-ui, -apple-system, sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; }
            .card { background: #0F172A; border: 1.5px solid #00E5FF; border-radius: 20px; padding: 36px; max-width: 480px; width: 90%; text-align: center; box-shadow: 0 10px 40px rgba(0,229,255,0.15); }
            .badge { background: rgba(16,185,129,0.2); color: #10B981; border: 1px solid #10B981; border-radius: 30px; padding: 4px 14px; font-weight: 700; font-size: 13px; display: inline-block; margin-bottom: 16px; }
            h1 { font-size: 24px; margin: 0 0 8px 0; color: #FFF; }
            p { color: #94A3B8; font-size: 14px; line-height: 1.6; margin: 8px 0; }
            .email-box { background: #1E293B; border-radius: 10px; padding: 12px; margin: 18px 0; font-weight: 600; color: #00E5FF; }
            .btn { display: inline-block; background: #00E5FF; color: #070C1B; font-weight: 700; padding: 12px 28px; border-radius: 12px; text-decoration: none; margin-top: 14px; }
            .close-note { color: #10B981; font-size: 13px; margin-top: 14px; display: none; }
          </style>
        </head>
        <body>
          <div class="card">
            <div class="badge">OAUTH 2.0 CONNECTED</div>
            <h1>Google Account Connected!</h1>
            <p>Your Google account has been authorized for <strong>NIAZ AI ASSISTANT</strong>.</p>
            <div class="email-box">${userEmail}</div>
            <p>Gmail API, Google Calendar API, and Google Drive API are now online and ready to execute real tasks.</p>
            <p id="closeMsg" class="close-note">Authorization successful! Closing popup and refreshing dashboard...</p>
            <a href="/" class="btn" id="portalBtn">Return to Dashboard</a>
          </div>

          <script>
            try {
              if (window.opener && !window.opener.closed) {
                document.getElementById('closeMsg').style.display = 'block';
                document.getElementById('portalBtn').style.display = 'none';
                window.opener.postMessage({ type: 'GOOGLE_OAUTH_SUCCESS', email: '${userEmail}' }, '*');
                setTimeout(function() {
                  window.close();
                }, 1800);
              }
            } catch (err) {
              console.log('Opener messaging error:', err);
            }
          </script>
        </body>
        </html>
      `);
      return;
    }

    // 4. Connection Status endpoint
    if (cleanPath === '/api/auth/google/status' && req.method === 'GET') {
      const tokenData = loadTokens();
      if (!tokenData) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          connected: false,
          status: 'Not Connected',
          email: null,
          connectedAt: null,
          scopes: []
        }));
        return;
      }

      // Check if access token is fresh or can be refreshed
      try {
        await getValidAccessToken();
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          connected: true,
          status: 'Connected',
          email: tokenData.email,
          name: tokenData.name,
          connectedAt: tokenData.connectedAt,
          scopes: ['Gmail API', 'Google Calendar API', 'Google Drive API']
        }));
      } catch (e) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          connected: false,
          status: 'Authorization Required',
          email: tokenData.email,
          error: e.message
        }));
      }
      return;
    }

    // 5. Disconnect Google endpoint
    if (cleanPath === '/api/auth/google/disconnect' && req.method === 'POST') {
      const tokenData = loadTokens();
      if (tokenData && tokenData.access_token) {
        try {
          await makeHttpsRequest({
            hostname: 'oauth2.googleapis.com',
            path: `/revoke?token=${encodeURIComponent(tokenData.access_token)}`,
            method: 'POST'
          });
        } catch (e) {
          console.warn('Revoke token warning:', e.message);
        }
      }
      deleteTokens();
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true, message: 'Google account disconnected successfully' }));
      return;
    }

    // 6. Reauthorize Google endpoint
    if (cleanPath === '/api/auth/google/reauthorize' && req.method === 'POST') {
      deleteTokens();
      const { clientId } = getCredentials();
      const redirectUri = getRedirectUri(req);
      const authUrl = `https://accounts.google.com/o/oauth2/v2/auth?` + querystring.stringify({
        client_id: clientId,
        redirect_uri: redirectUri,
        response_type: 'code',
        scope: SCOPES.join(' '),
        access_type: 'offline',
        prompt: 'consent'
      });
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true, authUrl }));
      return;
    }

    // ==========================================
    // TOOL ENDPOINTS (Real Google APIs)
    // ==========================================

    // GMAIL: search
    if (pathname === '/api/tools/gmail/search' && req.method === 'GET') {
      const accessToken = await getValidAccessToken();
      const q = parsedUrl.query.q || '';
      const maxResults = parsedUrl.query.maxResults || 10;

      const resp = await makeHttpsRequest({
        hostname: 'gmail.googleapis.com',
        path: `/gmail/v1/users/me/messages?q=${encodeURIComponent(q)}&maxResults=${maxResults}`,
        method: 'GET',
        headers: { 'Authorization': `Bearer ${accessToken}` }
      });

      const messageHeaders = [];
      const messages = (resp.data && resp.data.messages) ? resp.data.messages.slice(0, 5) : [];

      for (const m of messages) {
        try {
          const detail = await makeHttpsRequest({
            hostname: 'gmail.googleapis.com',
            path: `/gmail/v1/users/me/messages/${m.id}?format=metadata&metadataHeaders=Subject&metadataHeaders=From&metadataHeaders=Date`,
            method: 'GET',
            headers: { 'Authorization': `Bearer ${accessToken}` }
          });
          if (detail.data) {
            const headers = detail.data.payload ? (detail.data.payload.headers || []) : [];
            const sub = (headers.find(h => h.name === 'Subject') || {}).value || '(No Subject)';
            const from = (headers.find(h => h.name === 'From') || {}).value || 'Unknown';
            const date = (headers.find(h => h.name === 'Date') || {}).value || '';
            messageHeaders.push({ id: m.id, threadId: m.threadId, subject: sub, from, date, snippet: detail.data.snippet });
          }
        } catch (e) {}
      }

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        success: true,
        query: q,
        resultSizeEstimate: resp.data ? (resp.data.resultSizeEstimate || 0) : 0,
        messages: messageHeaders
      }));
      return;
    }

    // GMAIL: read
    if (pathname === '/api/tools/gmail/read' && req.method === 'GET') {
      const accessToken = await getValidAccessToken();
      const id = parsedUrl.query.id;
      if (!id) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Missing message id' }));
        return;
      }

      const resp = await makeHttpsRequest({
        hostname: 'gmail.googleapis.com',
        path: `/gmail/v1/users/me/messages/${id}?format=full`,
        method: 'GET',
        headers: { 'Authorization': `Bearer ${accessToken}` }
      });

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true, message: resp.data }));
      return;
    }

    // GMAIL: createDraft
    if (pathname === '/api/tools/gmail/createDraft' && req.method === 'POST') {
      const accessToken = await getValidAccessToken();
      const body = await parseBody(req);
      const to = body.to || '';
      const subject = body.subject || '(No Subject)';
      const emailBody = body.body || '';

      const rfcMessage = `To: ${to}\r\nSubject: ${subject}\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n${emailBody}`;
      const raw = Buffer.from(rfcMessage).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

      const postData = JSON.stringify({ message: { raw } });
      const resp = await makeHttpsRequest({
        hostname: 'gmail.googleapis.com',
        path: '/gmail/v1/users/me/drafts',
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(postData)
        }
      }, postData);

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: resp.statusCode === 200, draft: resp.data }));
      return;
    }

    // GMAIL: send
    if (pathname === '/api/tools/gmail/send' && req.method === 'POST') {
      const accessToken = await getValidAccessToken();
      const body = await parseBody(req);
      const to = body.to || '';
      const subject = body.subject || '(No Subject)';
      const emailBody = body.body || '';

      const rfcMessage = `To: ${to}\r\nSubject: ${subject}\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n${emailBody}`;
      const raw = Buffer.from(rfcMessage).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

      const postData = JSON.stringify({ raw });
      const resp = await makeHttpsRequest({
        hostname: 'gmail.googleapis.com',
        path: '/gmail/v1/users/me/messages/send',
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(postData)
        }
      }, postData);

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: resp.statusCode === 200, result: resp.data }));
      return;
    }

    // CALENDAR: list
    if (pathname === '/api/tools/calendar/list' && req.method === 'GET') {
      const accessToken = await getValidAccessToken();
      const timeMin = parsedUrl.query.timeMin || new Date().toISOString();
      const maxResults = parsedUrl.query.maxResults || 10;

      const resp = await makeHttpsRequest({
        hostname: 'www.googleapis.com',
        path: `/calendar/v3/calendars/primary/events?timeMin=${encodeURIComponent(timeMin)}&maxResults=${maxResults}&singleEvents=true&orderBy=startTime`,
        method: 'GET',
        headers: { 'Authorization': `Bearer ${accessToken}` }
      });

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true, events: (resp.data && resp.data.items) || [] }));
      return;
    }

    // CALENDAR: create
    if (pathname === '/api/tools/calendar/create' && req.method === 'POST') {
      const accessToken = await getValidAccessToken();
      const body = await parseBody(req);
      const summary = body.summary || 'Meeting with NIAZ AI';
      const description = body.description || 'Created via NIAZ AI ASSISTANT';
      const start = body.start || { dateTime: new Date(Date.now() + 3600000).toISOString() };
      const end = body.end || { dateTime: new Date(Date.now() + 7200000).toISOString() };
      const attendees = body.attendees || [];

      const postData = JSON.stringify({ summary, description, start, end, attendees });
      const resp = await makeHttpsRequest({
        hostname: 'www.googleapis.com',
        path: '/calendar/v3/calendars/primary/events',
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(postData)
        }
      }, postData);

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: resp.statusCode === 200, event: resp.data }));
      return;
    }

    // CALENDAR: update
    if (pathname === '/api/tools/calendar/update' && req.method === 'POST') {
      const accessToken = await getValidAccessToken();
      const body = await parseBody(req);
      const eventId = body.eventId;
      if (!eventId) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Missing eventId' }));
        return;
      }

      const postData = JSON.stringify(body);
      const resp = await makeHttpsRequest({
        hostname: 'www.googleapis.com',
        path: `/calendar/v3/calendars/primary/events/${encodeURIComponent(eventId)}`,
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(postData)
        }
      }, postData);

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: resp.statusCode === 200, event: resp.data }));
      return;
    }

    // CALENDAR: delete
    if (pathname === '/api/tools/calendar/delete' && req.method === 'POST') {
      const accessToken = await getValidAccessToken();
      const body = await parseBody(req);
      const eventId = body.eventId;
      if (!eventId) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Missing eventId' }));
        return;
      }

      const resp = await makeHttpsRequest({
        hostname: 'www.googleapis.com',
        path: `/calendar/v3/calendars/primary/events/${encodeURIComponent(eventId)}`,
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${accessToken}` }
      });

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: resp.statusCode === 204 || resp.statusCode === 200, eventId }));
      return;
    }

    // DRIVE: search
    if (pathname === '/api/tools/drive/search' && req.method === 'GET') {
      const accessToken = await getValidAccessToken();
      const q = parsedUrl.query.q || '';
      const safeQuery = q ? `name contains '${q.replace(/'/g, "\\'")}' and trashed = false` : 'trashed = false';

      const resp = await makeHttpsRequest({
        hostname: 'www.googleapis.com',
        path: `/drive/v3/files?q=${encodeURIComponent(safeQuery)}&fields=files(id,name,mimeType,modifiedTime,size,webViewLink)`,
        method: 'GET',
        headers: { 'Authorization': `Bearer ${accessToken}` }
      });

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true, files: (resp.data && resp.data.files) || [] }));
      return;
    }

    // DRIVE: list
    if (pathname === '/api/tools/drive/list' && req.method === 'GET') {
      const accessToken = await getValidAccessToken();
      const pageSize = parsedUrl.query.pageSize || 15;

      const resp = await makeHttpsRequest({
        hostname: 'www.googleapis.com',
        path: `/drive/v3/files?pageSize=${pageSize}&fields=files(id,name,mimeType,modifiedTime,size,webViewLink)&orderBy=modifiedTime desc`,
        method: 'GET',
        headers: { 'Authorization': `Bearer ${accessToken}` }
      });

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true, files: (resp.data && resp.data.files) || [] }));
      return;
    }

    // DRIVE: upload
    if (pathname === '/api/tools/drive/upload' && req.method === 'POST') {
      const accessToken = await getValidAccessToken();
      const body = await parseBody(req);
      const name = body.name || 'Document_from_Niaz_AI.txt';
      const content = body.content || 'Generated report from NIAZ AI ASSISTANT';
      const mimeType = body.mimeType || 'text/plain';

      const boundary = '-------314159265358979323846';
      const delimiter = "\r\n--" + boundary + "\r\n";
      const closeDelim = "\r\n--" + boundary + "--";

      const metadata = JSON.stringify({ name, mimeType });
      const multipartRequestBody =
        delimiter +
        'Content-Type: application/json; charset=UTF-8\r\n\r\n' +
        metadata +
        delimiter +
        'Content-Type: ' + mimeType + '\r\n\r\n' +
        content +
        closeDelim;

      const resp = await makeHttpsRequest({
        hostname: 'www.googleapis.com',
        path: '/upload/drive/v3/files?uploadType=multipart&fields=id,name,webViewLink',
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'multipart/related; boundary=' + boundary,
          'Content-Length': Buffer.byteLength(multipartRequestBody)
        }
      }, multipartRequestBody);

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: resp.statusCode === 200, file: resp.data }));
      return;
    }

    // ==========================================
    // ROOT / DASHBOARD / DIAGNOSTIC UI: OAuth 2.0 Management Page
    // ==========================================
    if (cleanPath === '/' || cleanPath === '/oauth' || cleanPath === '/diagnostic' || cleanPath === '/oauth/diagnostic') {
      const diag = getOAuthDiagnostics();

      // If requested as JSON explicitly on diagnostic page
      if ((cleanPath === '/diagnostic' || cleanPath === '/oauth/diagnostic') && req.headers.accept && req.headers.accept.includes('application/json')) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          googleClientIdConfigured: diag.googleClientIdConfigured,
          googleClientSecretConfigured: diag.googleClientSecretConfigured
        }));
        return;
      }

      const { clientId, clientSecret } = getCredentials();
      const redirectUri = getRedirectUri(req);
      const tokenData = loadTokens();
      const isConnected = Boolean(tokenData && tokenData.connected);
      const email = tokenData ? tokenData.email : 'Not connected';

      let authUrl = '#';
      if (diag.clientIdConfigured && diag.clientSecretConfigured) {
        const state = generateOAuthState();
        authUrl = 'https://accounts.google.com/o/oauth2/v2/auth?' + querystring.stringify({
          client_id: clientId,
          redirect_uri: redirectUri,
          response_type: 'code',
          scope: SCOPES.join(' '),
          access_type: 'offline',
          prompt: 'consent',
          state: state
        });
      }

      const clientIdDisplay = diag.clientIdMasked;
      const lastSyncDisplay = tokenData && tokenData.connectedAt ? new Date(tokenData.connectedAt).toLocaleString() : 'Never';
      const connStatusColor = isConnected ? 'var(--green)' : 'var(--amber)';
      const connStatusText = isConnected ? 'Connected & Active' : 'Not Connected';
      const badgeClass = isConnected ? 'badge-connected' : 'badge-disconnected';
      const badgeText = isConnected ? '● CONNECTED' : '○ NOT CONNECTED';

      let actionsHtml = '';
      if (!diag.clientIdConfigured || !diag.clientSecretConfigured) {
        actionsHtml = '<div style="color:var(--amber);font-size:14px;background:rgba(245,158,11,0.1);border:1px solid var(--amber);padding:14px;border-radius:12px;width:100%;">' +
          '<strong>Google OAuth Not Configured:</strong> Set <code>GOOGLE_CLIENT_ID</code> and <code>GOOGLE_CLIENT_SECRET</code> in server environment variables or AI Studio Secrets.' +
          '</div>';
      } else if (!isConnected) {
        actionsHtml = '<a href="' + authUrl + '" target="_blank" rel="noopener noreferrer" class="btn btn-primary" id="connectGoogleBtn">' +
          '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:8px;vertical-align:middle;"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>' +
          'Connect Google (New Tab)</a>' +
          '<button type="button" onclick="openOAuthPopup(\'' + authUrl + '\')" class="btn btn-outline" style="font-size:13px;" id="popupBtn">Open in Popup</button>' +
          '<button type="button" onclick="copyAuthUrl(\'' + authUrl + '\')" class="btn btn-outline" style="font-size:13px;" id="copyLinkBtn">Copy Link</button>';
      } else {
        actionsHtml = '<button onclick="disconnectGoogle()" class="btn btn-danger">Disconnect Google</button>' +
          '<a href="' + authUrl + '" target="_blank" rel="noopener noreferrer" class="btn btn-outline" id="reauthGoogleBtn">' +
          '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:6px;vertical-align:middle;"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>' +
          'Reauthorize Account</a>' +
          '<button type="button" onclick="copyAuthUrl(\'' + authUrl + '\')" class="btn btn-outline" style="font-size:13px;">Copy Link</button>';
      }

      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(`
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>Google OAuth 2.0 & Diagnostics — NIAZ AI ASSISTANT</title>
          <style>
            :root {
              --bg: #070C1B;
              --card: #0F172A;
              --border: #1E2E5C;
              --cyan: #00E5FF;
              --blue: #2563EB;
              --green: #10B981;
              --amber: #F59E0B;
              --red: #EF4444;
              --text: #F8FAFC;
              --muted: #94A3B8;
            }
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body { background: var(--bg); color: var(--text); font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 24px 16px; line-height: 1.5; }
            .container { max-width: 860px; margin: 0 auto; }
            .header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid var(--border); }
            .logo-wrap { display: flex; align-items: center; gap: 12px; }
            .logo-icon { width: 44px; height: 44px; border-radius: 50%; background: #0F1B3B; border: 2px solid var(--cyan); display: flex; align-items: center; justify-content: center; font-size: 22px; font-weight: 900; color: var(--cyan); }
            .title { font-size: 20px; font-weight: 800; letter-spacing: 0.5px; }
            .subtitle { font-size: 13px; color: var(--muted); }
            .card { background: var(--card); border: 1px solid var(--border); border-radius: 16px; padding: 20px; margin-bottom: 20px; }
            .badge { display: inline-flex; align-items: center; gap: 6px; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 700; }
            .badge-connected { background: rgba(16,185,129,0.15); color: var(--green); border: 1px solid var(--green); }
            .badge-disconnected { background: rgba(148,163,184,0.15); color: var(--muted); border: 1px solid var(--muted); }
            .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px; margin-top: 14px; }
            .info-item { background: #070C1B; border: 1px solid var(--border); border-radius: 12px; padding: 14px; }
            .info-label { font-size: 11px; text-transform: uppercase; color: var(--muted); font-weight: 700; margin-bottom: 4px; }
            .info-val { font-size: 14px; font-weight: 600; color: #FFF; word-break: break-all; }
            .ready { color: var(--green); }
            .err-val { color: var(--red); }
            .actions-bar { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 20px; align-items: center; }
            .btn { display: inline-flex; align-items: center; justify-content: center; padding: 12px 24px; border-radius: 12px; font-weight: 700; font-size: 14px; cursor: pointer; text-decoration: none; border: none; transition: 0.2s; }
            .btn-primary { background: var(--cyan); color: #070C1B; }
            .btn-primary:hover { opacity: 0.9; }
            .btn-danger { background: rgba(239,68,68,0.15); color: var(--red); border: 1px solid var(--red); }
            .btn-outline { background: transparent; color: #FFF; border: 1px solid var(--border); }
            .uri-box { background: #070C1B; border: 1px dashed var(--cyan); padding: 14px; border-radius: 10px; font-family: monospace; font-size: 13px; color: var(--cyan); word-break: break-all; margin: 10px 0; display: flex; align-items: center; justify-content: space-between; gap: 10px; }
            .copy-btn { background: var(--cyan); color: #070C1B; border: none; padding: 6px 12px; border-radius: 6px; font-weight: 700; cursor: pointer; font-size: 11px; flex-shrink: 0; }
            .tool-section { margin-top: 24px; }
            .tool-item { background: #070C1B; border: 1px solid var(--border); border-radius: 10px; padding: 12px; margin-bottom: 10px; display: flex; align-items: center; justify-content: space-between; }
            .result-pre { background: #070C1B; padding: 12px; border-radius: 8px; border: 1px solid var(--border); max-height: 200px; overflow-y: auto; font-family: monospace; font-size: 12px; color: #A5F3FC; margin-top: 8px; display: none; }
          </style>
        </head>
        <body>
          <div class="container">
            <header class="header">
              <div class="logo-wrap">
                <div class="logo-icon">N</div>
                <div>
                  <h1 class="title">NIAZ AI ASSISTANT</h1>
                  <p class="subtitle">Official Google OAuth 2.0 & Services Gateway • Niaz Ahmed</p>
                </div>
              </div>
              <div>
                <span class="badge ${badgeClass}">
                  ${badgeText}
                </span>
              </div>
            </header>

            <!-- DIAGNOSTIC AUDIT CARD (Shows ONLY Safe Information, No Secrets Revealed) -->
            <div class="card" style="border: 1.5px solid var(--cyan);">
              <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:14px;flex-wrap:wrap;gap:10px;">
                <div>
                  <h2 style="font-size: 17px; color: #FFF; display:flex; align-items:center; gap:8px;">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="color:var(--cyan);"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
                    Google OAuth Configuration Diagnostics
                  </h2>
                  <p style="font-size:12px;color:var(--muted);">Safe verification audit • Client Secret value protected</p>
                </div>
                <div>
                  <span class="badge ${diag.googleOAuthStatus === 'Configured ✓' ? 'badge-connected' : 'badge-disconnected'}">
                    Google OAuth: ${diag.googleOAuthStatus}
                  </span>
                </div>
              </div>

              <div class="grid">
                <div class="info-item">
                  <div class="info-label">Google OAuth</div>
                  <div class="info-val ${diag.googleOAuthStatus === 'Configured ✓' ? 'ready' : 'err-val'}">${diag.googleOAuthStatus}</div>
                </div>
                <div class="info-item">
                  <div class="info-label">Client ID</div>
                  <div class="info-val ${diag.clientIdConfigured ? 'ready' : 'err-val'}">${diag.clientIdStatus} <span style="font-size:11px;color:var(--muted);display:block;margin-top:2px;">(${diag.clientIdMasked})</span></div>
                </div>
                <div class="info-item">
                  <div class="info-label">Client Secret</div>
                  <div class="info-val ${diag.clientSecretConfigured ? 'ready' : 'err-val'}">${diag.clientSecretStatus} <span style="font-size:11px;color:var(--green);display:block;margin-top:2px;">(Server secret — value hidden)</span></div>
                </div>
                <div class="info-item">
                  <div class="info-label">OAuth Callback</div>
                  <div class="info-val ready">${diag.oauthCallbackStatus} <span style="font-size:11px;color:var(--muted);display:block;margin-top:2px;">(/api/auth/google/callback)</span></div>
                </div>
              </div>

              <div style="margin-top:14px;padding:12px;border-radius:10px;background:${diag.multipleClientIdsDetected ? 'rgba(239,68,68,0.12)' : 'rgba(16,185,129,0.12)'};border:1px solid ${diag.multipleClientIdsDetected ? 'var(--red)' : 'var(--green)'};font-size:13px;line-height:1.5;">
                <strong style="color:${diag.multipleClientIdsDetected ? 'var(--red)' : 'var(--green)'};">${diag.multipleClientIdsDetected ? '⚠️ DIAGNOSTIC WARNING:' : '✓ CONFIGURATION AUDIT:'}</strong>
                <span style="color:#FFF;margin-left:6px;">${diag.diagnosticMessage}</span>
              </div>

              <div style="margin-top:14px;font-size:12px;color:var(--muted);display:flex;flex-wrap:wrap;gap:16px;">
                <div><strong>Active Source:</strong> <span style="color:var(--cyan);">${diag.configSource}</span></div>
                <div><strong>Expected Env:</strong> <code style="color:var(--cyan);">GOOGLE_CLIENT_ID</code>, <code style="color:var(--cyan);">GOOGLE_CLIENT_SECRET</code></div>
                <div><strong>Callback Route:</strong> <code style="color:var(--cyan);">/api/auth/google/callback</code></div>
              </div>
            </div>

            <div class="card">
              <h2 style="font-size: 17px; margin-bottom: 12px; color: #FFF;">Google Account Connection Status</h2>
              <div class="grid">
                <div class="info-item">
                  <div class="info-label">Account Email</div>
                  <div class="info-val" style="color:var(--cyan);">${email}</div>
                </div>
                <div class="info-item">
                  <div class="info-label">Connection Status</div>
                  <div class="info-val" style="color:${connStatusColor};">
                    ${connStatusText}
                  </div>
                </div>
                <div class="info-item">
                  <div class="info-label">Last Synchronized</div>
                  <div class="info-val">${lastSyncDisplay}</div>
                </div>
              </div>

              <div class="actions-bar">
                ${actionsHtml}
              </div>
            </div>

            <div class="card">
              <h2 style="font-size: 17px; margin-bottom: 6px; color: #FFF;">Production Authorized Redirect URI & Google Services</h2>
              <p style="font-size: 13px; color: var(--muted); margin-bottom: 14px;">
                Confirm this exact URI is saved under <strong>Google Cloud Console &rarr; APIs & Services &rarr; Credentials &rarr; Authorized redirect URIs</strong> for your Web Application OAuth client:
              </p>

              <div class="uri-box">
                <span id="redirectUriText">${redirectUri}</span>
                <button class="copy-btn" onclick="copyUri()">COPY URI</button>
              </div>

              <div class="grid" style="margin-top: 16px;">
                <div class="info-item">
                  <div class="info-label">Gmail API</div>
                  <div class="info-val ready">Ready (modify, send, read)</div>
                </div>
                <div class="info-item">
                  <div class="info-label">Google Calendar API</div>
                  <div class="info-val ready">Ready (events, create, delete)</div>
                </div>
                <div class="info-item">
                  <div class="info-label">Google Drive API</div>
                  <div class="info-val ready">Ready (search, list, upload)</div>
                </div>
              </div>
            </div>

            <div class="card tool-section">
              <h2 style="font-size: 17px; margin-bottom: 12px; color: #FFF;">Live Backend Tool Inspector</h2>
              <p style="font-size: 13px; color: var(--muted); margin-bottom: 16px;">Test the real backend tool endpoints connected to your Google account:</p>

              <div class="tool-item">
                <div>
                  <strong>gmail.search</strong>
                  <div style="font-size:12px;color:var(--muted);">Search user inbox for recent messages</div>
                </div>
                <button class="btn btn-outline" style="padding:6px 14px;font-size:12px;" onclick="testTool('/api/tools/gmail/search', 'gmailRes')">Test Search</button>
              </div>
              <pre id="gmailRes" class="result-pre"></pre>

              <div class="tool-item">
                <div>
                  <strong>calendar.list</strong>
                  <div style="font-size:12px;color:var(--muted);">List upcoming events from primary calendar</div>
                </div>
                <button class="btn btn-outline" style="padding:6px 14px;font-size:12px;" onclick="testTool('/api/tools/calendar/list', 'calRes')">Test Calendar</button>
              </div>
              <pre id="calRes" class="result-pre"></pre>

              <div class="tool-item">
                <div>
                  <strong>drive.list</strong>
                  <div style="font-size:12px;color:var(--muted);">List recent files in Google Drive</div>
                </div>
                <button class="btn btn-outline" style="padding:6px 14px;font-size:12px;" onclick="testTool('/api/tools/drive/list', 'driveRes')">Test Drive</button>
              </div>
              <pre id="driveRes" class="result-pre"></pre>
            </div>
          </div>

          <script>
            function openOAuthPopup(url) {
              const width = 600;
              const height = 720;
              const left = Math.max(0, Math.round((window.screen.width - width) / 2));
              const top = Math.max(0, Math.round((window.screen.height - height) / 2));

              try {
                const popup = window.open(
                  url,
                  'GoogleOAuthPopup',
                  'width=' + width + ',height=' + height + ',top=' + top + ',left=' + left + ',scrollbars=yes,status=yes,resizable=yes'
                );
                if (popup && !popup.closed) {
                  popup.focus();
                  return;
                }
              } catch (e) {
                console.warn('Popup blocked, opening in new tab instead:', e);
              }
              window.open(url, '_blank', 'noopener,noreferrer');
            }

            function copyAuthUrl(url) {
              if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(url).then(function() {
                  alert('Google Sign-In URL copied to clipboard! Paste it into a new browser tab.');
                }).catch(function() {
                  prompt('Copy this Google OAuth URL:', url);
                });
              } else {
                prompt('Copy this Google OAuth URL:', url);
              }
            }

            // Automatically reload dashboard when popup OAuth completes
            window.addEventListener('message', function(event) {
              if (event.data && event.data.type === 'GOOGLE_OAUTH_SUCCESS') {
                console.log('OAuth authorization completed for:', event.data.email);
                setTimeout(function() {
                  window.location.reload();
                }, 1000);
              }
            });

            function copyUri() {
              const text = document.getElementById('redirectUriText').innerText;
              navigator.clipboard.writeText(text);
              alert('Copied to clipboard: ' + text);
            }

            async function disconnectGoogle() {
              if (!confirm('Are you sure you want to disconnect Google from NIAZ AI ASSISTANT?')) return;
              const res = await fetch('/api/auth/google/disconnect', { method: 'POST' });
              if (res.ok) {
                location.reload();
              } else {
                alert('Disconnect failed');
              }
            }

            async function testTool(endpoint, resultId) {
              const el = document.getElementById(resultId);
              el.style.display = 'block';
              el.innerText = 'Calling ' + endpoint + '...';
              try {
                const res = await fetch(endpoint);
                const data = await res.json();
                el.innerText = JSON.stringify(data, null, 2);
              } catch (e) {
                el.innerText = 'Error: ' + e.message;
              }
            }
          </script>
        </body>
        </html>
      `);
      return;
    }

    // Default 404
    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Endpoint not found' }));
  } catch (err) {
    console.error('Server error:', err);
    res.writeHead(500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: err.message }));
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`NIAZ AI Backend & OAuth 2.0 Server listening on port ${PORT}`);
});
