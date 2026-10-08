# Backend Server Setup & Deployment Guide

This guide explains how to install, configure, run, and expose the **PhoneBridge Backend** server.

---

## 1. Prerequisites

- **Node.js**: Version 18.0.0 or newer (tested on Node v20/v24).
- **npm**: Version 9.0.0 or newer.

---

## 2. Installation

1. Open your terminal in the `backend/` directory:
   ```bash
   cd e:\Code\Projects\alexa\backend
   ```
2. Install npm dependencies:
   ```bash
   npm install
   ```

---

## 3. Configuration (`.env`)

1. Verify or copy the template:
   ```bash
   cp .env.example .env
   ```
2. Edit `.env` to customize your configuration:

```env
# Port the HTTP & WebSocket server will listen on
PORT=3000
HOST=0.0.0.0

# Device Authentication Token
# Must match the token entered in the Android App Settings!
DEVICE_TOKEN=dev_token_secret_123

# Alexa Integration Secret
# Must match the secret in alexa-skill/lambda or your Lambda environment variables!
ALEXA_CLIENT_SECRET=alexa_skill_secret_456

# Optional: Set a target device ID if multiple Android phones connect
DEFAULT_DEVICE_ID=

NODE_ENV=development
```

---

## 4. Running the Server

### For Development (with Auto-Reload):
```bash
npm run dev
```

### For Production:
```bash
npm start
```

### Verifying Server Health:
Open your browser or run:
```bash
curl http://localhost:3000/api/health
```

Expected response:
```json
{
  "status": "ok",
  "service": "PhoneBridge Backend",
  "uptime": 12,
  "timestamp": "2026-10-08T11:15:00.000Z",
  "connectedDevices": 1
}
```

---

## 5. Exposing the Backend via HTTPS for Alexa

Amazon Alexa Skills require all external endpoints to use **HTTPS with a valid SSL certificate**. For local development and testing, you can expose your local server using a secure tunnel:

### Option A: Using ngrok (Recommended)
1. Download [ngrok](https://ngrok.com/) if you haven't already.
2. In a separate terminal, run:
   ```bash
   ngrok http 3000
   ```
3. Copy the public forwarding HTTPS URL (e.g., `https://abc-123.ngrok-free.app`).
4. This URL is your `BACKEND_URL` for the Alexa Skill Lambda and Android app!

### Option B: Using Cloudflare Tunnel
```bash
cloudflared tunnel --url http://localhost:3000
```

### Option C: Using localtunnel
```bash
npx localtunnel --port 3000
```

---

## 6. Running Automated Tests

To run the complete test suite verifying authentication, state machine transitions, and WebSocket commands:

```bash
npm test
```
