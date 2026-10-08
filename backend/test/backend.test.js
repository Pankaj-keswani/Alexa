const test = require('node:test');
const assert = require('node:assert/strict');
const WebSocket = require('ws');
const { app, server } = require('../src/server');

const TEST_PORT = 3001;
const BASE_URL = `http://127.0.0.1:${TEST_PORT}`;
const WS_URL = `ws://127.0.0.1:${TEST_PORT}/ws`;
const DEVICE_TOKEN = 'dev_token_secret_123';
const ALEXA_SECRET = 'alexa_skill_secret_456';

test.before(async () => {
  await new Promise((resolve) => {
    server.listen(TEST_PORT, '127.0.0.1', () => {
      resolve();
    });
  });
});

test.after(async () => {
  const websocketService = require('../src/services/websocketService');
  websocketService.cleanup();
  await new Promise((resolve) => {
    server.close(() => {
      resolve();
    });
  });
});

test('GET /api/health should return ok status', async () => {
  const res = await fetch(`${BASE_URL}/api/health`);
  assert.equal(res.status, 200);
  const data = await res.json();
  assert.equal(data.status, 'ok');
  assert.equal(typeof data.uptime, 'number');
});

test('POST /api/events/call without auth should return 401', async () => {
  const res = await fetch(`${BASE_URL}/api/events/call`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ event: 'INCOMING_CALL' })
  });
  assert.equal(res.status, 401);
});

test('POST /api/events/call with valid device token should update call status', async () => {
  // 1. Send INCOMING_CALL
  const res = await fetch(`${BASE_URL}/api/events/call`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-device-token': DEVICE_TOKEN,
      'x-device-id': 'test-phone-1'
    },
    body: JSON.stringify({
      deviceId: 'test-phone-1',
      event: 'INCOMING_CALL',
      callerName: 'Rahul',
      phoneNumber: '+919876543210',
      timestamp: new Date().toISOString()
    })
  });
  assert.equal(res.status, 200);
  const data = await res.json();
  assert.equal(data.success, true);
  assert.equal(data.currentState.state, 'RINGING');
  assert.equal(data.currentState.callerName, 'Rahul');

  // 2. Query status with Alexa secret
  const statusRes = await fetch(`${BASE_URL}/api/call/status`, {
    headers: { 'x-alexa-secret': ALEXA_SECRET }
  });
  assert.equal(statusRes.status, 200);
  const statusData = await statusRes.json();
  assert.equal(statusData.hasActiveCall, true);
  assert.equal(statusData.state, 'RINGING');
  assert.equal(statusData.callerName, 'Rahul');

  // 3. Send CALL_ENDED
  const endRes = await fetch(`${BASE_URL}/api/events/call`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-device-token': DEVICE_TOKEN
    },
    body: JSON.stringify({
      deviceId: 'test-phone-1',
      event: 'CALL_ENDED'
    })
  });
  assert.equal(endRes.status, 200);

  // 4. Verify status is IDLE
  const statusRes2 = await fetch(`${BASE_URL}/api/call/status`, {
    headers: { 'x-alexa-secret': ALEXA_SECRET }
  });
  const statusData2 = await statusRes2.json();
  assert.equal(statusData2.hasActiveCall, false);
  assert.equal(statusData2.state, 'IDLE');
});

test('WebSocket connection and command execution flow', async () => {
  const ws = new WebSocket(`${WS_URL}?token=${DEVICE_TOKEN}&deviceId=test-phone-1`);

  await new Promise((resolve, reject) => {
    ws.on('open', resolve);
    ws.on('error', reject);
  });

  // Listen for incoming commands from backend
  ws.on('message', (raw) => {
    const msg = JSON.parse(raw.toString());
    if (msg.command === 'ANSWER_CALL') {
      // Simulate Android answering and returning success
      ws.send(JSON.stringify({
        requestId: msg.requestId,
        command: msg.command,
        success: true,
        reason: 'Simulated Telecom answer'
      }));
    }
  });

  // Set call to RINGING first
  await fetch(`${BASE_URL}/api/events/call`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-device-token': DEVICE_TOKEN
    },
    body: JSON.stringify({
      deviceId: 'test-phone-1',
      event: 'INCOMING_CALL',
      callerName: 'Rahul',
      phoneNumber: '+919876543210'
    })
  });

  // Send answer command from Alexa endpoint
  const answerRes = await fetch(`${BASE_URL}/api/call/answer`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-alexa-secret': ALEXA_SECRET
    },
    body: JSON.stringify({ deviceId: 'test-phone-1' })
  });

  assert.equal(answerRes.status, 200);
  const answerData = await answerRes.json();
  assert.equal(answerData.success, true);
  assert.equal(answerData.message, 'Call answered successfully.');

  ws.close();
});
