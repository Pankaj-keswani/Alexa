/**
 * Client for communicating with the PhoneBridge Node.js Backend.
 * Uses environment variables:
 * - BACKEND_URL: Base URL of PhoneBridge backend (e.g., https://xyz.ngrok-free.app)
 * - ALEXA_CLIENT_SECRET: Pre-shared secret matching backend .env
 */

const BACKEND_URL = (process.env.BACKEND_URL || 'https://uninsured-unrelated-simmering.ngrok-free.dev').replace(/\/$/, '');
const ALEXA_SECRET = process.env.ALEXA_CLIENT_SECRET || 'alexa_skill_secret_456';
const TIMEOUT_MS = parseInt(process.env.BACKEND_TIMEOUT_MS, 10) || 5000;

async function request(endpoint, method = 'GET', body = null) {
  const url = `${BACKEND_URL}${endpoint}`;
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);

  try {
    const options = {
      method,
      headers: {
        'Content-Type': 'application/json',
        'x-alexa-secret': ALEXA_SECRET,
        'ngrok-skip-browser-warning': 'true',
        'User-Agent': 'PhoneBridge-Alexa'
      },
      signal: controller.signal
    };

    if (body) {
      options.body = JSON.stringify(body);
    }

    const response = await fetch(url, options);
    clearTimeout(timeoutId);

    const data = await response.json().catch(() => null);
    return {
      ok: response.ok,
      status: response.status,
      data
    };
  } catch (err) {
    clearTimeout(timeoutId);
    console.error(`Backend request failed (${method} ${endpoint}):`, err.message);
    return {
      ok: false,
      status: 0,
      error: err.name === 'AbortError' ? 'TIMEOUT' : err.message
    };
  }
}

const backendClient = {
  async getCallStatus() {
    return request('/api/call/status', 'GET');
  },

  async answerCall(deviceId = null) {
    return request('/api/call/answer', 'POST', { deviceId });
  },

  async rejectCall(deviceId = null) {
    return request('/api/call/reject', 'POST', { deviceId });
  }
};

module.exports = backendClient;
