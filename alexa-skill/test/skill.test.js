const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');

// Mock backend server for testing Alexa skill lambda
let mockBackendState = {
  hasActiveCall: false,
  state: 'IDLE',
  callerName: null,
  phoneNumber: null
};

let mockServer;
const MOCK_PORT = 3099;

function buildAlexaRequest(intentName, requestType = 'IntentRequest') {
  return {
    version: '1.0',
    session: {
      new: true,
      sessionId: 'amzn1.echo-api.session.test-session',
      application: { applicationId: 'amzn1.ask.skill.test' },
      user: { userId: 'amzn1.ask.account.test' }
    },
    request: requestType === 'LaunchRequest' ? {
      type: 'LaunchRequest',
      requestId: 'amzn1.echo-api.request.test-launch',
      timestamp: new Date().toISOString(),
      locale: 'en-IN'
    } : {
      type: 'IntentRequest',
      requestId: `amzn1.echo-api.request.test-${intentName}`,
      timestamp: new Date().toISOString(),
      locale: 'en-IN',
      intent: {
        name: intentName,
        confirmationStatus: 'NONE'
      }
    }
  };
}

test.before(async () => {
  mockServer = http.createServer((req, res) => {
    res.setHeader('Content-Type', 'application/json');

    if (req.url === '/api/call/status') {
      return res.end(JSON.stringify({
        success: true,
        ...mockBackendState
      }));
    }

    if (req.url === '/api/call/answer' && req.method === 'POST') {
      if (mockBackendState.state === 'RINGING') {
        mockBackendState.state = 'ACTIVE';
        return res.end(JSON.stringify({ success: true, message: 'Call answered' }));
      }
      return res.end(JSON.stringify({ success: false, reason: 'NO_INCOMING_CALL' }));
    }

    if (req.url === '/api/call/reject' && req.method === 'POST') {
      if (mockBackendState.state === 'RINGING') {
        mockBackendState.state = 'IDLE';
        mockBackendState.hasActiveCall = false;
        return res.end(JSON.stringify({ success: true, message: 'Call rejected' }));
      }
      return res.end(JSON.stringify({ success: false, reason: 'NO_INCOMING_CALL' }));
    }

    res.statusCode = 404;
    res.end(JSON.stringify({ error: 'Not found' }));
  });

  await new Promise((resolve) => mockServer.listen(MOCK_PORT, '127.0.0.1', resolve));

  process.env.BACKEND_URL = `http://127.0.0.1:${MOCK_PORT}`;
  process.env.ALEXA_CLIENT_SECRET = 'alexa_skill_secret_456';
});

test.after(async () => {
  await new Promise((resolve) => mockServer.close(resolve));
});

test('Alexa Skill - GetCallerIntent when no call', async () => {
  mockBackendState = { hasActiveCall: false, state: 'IDLE', callerName: null };
  const { handler } = require('../lambda/index');

  const response = await new Promise((resolve, reject) => {
    handler(buildAlexaRequest('GetCallerIntent'), {}, (err, res) => {
      if (err) reject(err); else resolve(res);
    });
  });

  const speech = response.response.outputSpeech.ssml;
  assert.match(speech, /There is no incoming call right now/);
});

test('Alexa Skill - GetCallerIntent when Rahul is calling', async () => {
  mockBackendState = { hasActiveCall: true, state: 'RINGING', callerName: 'Rahul', phoneNumber: '+919876543210' };
  const { handler } = require('../lambda/index');

  const response = await new Promise((resolve, reject) => {
    handler(buildAlexaRequest('GetCallerIntent'), {}, (err, res) => {
      if (err) reject(err); else resolve(res);
    });
  });

  const speech = response.response.outputSpeech.ssml;
  assert.match(speech, /You have an incoming call from Rahul/);
});

test('Alexa Skill - AnswerCallIntent when call is ringing', async () => {
  mockBackendState = { hasActiveCall: true, state: 'RINGING', callerName: 'Rahul' };
  const { handler } = require('../lambda/index');

  const response = await new Promise((resolve, reject) => {
    handler(buildAlexaRequest('AnswerCallIntent'), {}, (err, res) => {
      if (err) reject(err); else resolve(res);
    });
  });

  const speech = response.response.outputSpeech.ssml;
  assert.match(speech, /Okay, I answered the call/);
});

test('Alexa Skill - RejectCallIntent when call is ringing', async () => {
  mockBackendState = { hasActiveCall: true, state: 'RINGING', callerName: 'Rahul' };
  const { handler } = require('../lambda/index');

  const response = await new Promise((resolve, reject) => {
    handler(buildAlexaRequest('RejectCallIntent'), {}, (err, res) => {
      if (err) reject(err); else resolve(res);
    });
  });

  const speech = response.response.outputSpeech.ssml;
  assert.match(speech, /Okay, I rejected the call/);
});

test('Alexa Skill - AnswerCallIntent when no call to answer', async () => {
  mockBackendState = { hasActiveCall: false, state: 'IDLE', callerName: null };
  const { handler } = require('../lambda/index');

  const response = await new Promise((resolve, reject) => {
    handler(buildAlexaRequest('AnswerCallIntent'), {}, (err, res) => {
      if (err) reject(err); else resolve(res);
    });
  });

  const speech = response.response.outputSpeech.ssml;
  assert.match(speech, /There is no incoming call to answer/);
});

test('Alexa Skill - LaunchRequest greeting', async () => {
  mockBackendState = { hasActiveCall: false, state: 'IDLE', callerName: null };
  const { handler } = require('../lambda/index');

  const response = await new Promise((resolve, reject) => {
    handler(buildAlexaRequest(null, 'LaunchRequest'), {}, (err, res) => {
      if (err) reject(err); else resolve(res);
    });
  });

  const speech = response.response.outputSpeech.ssml;
  assert.match(speech, /Welcome to Phone Bridge/);
});
