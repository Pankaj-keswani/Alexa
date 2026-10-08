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

function buildAlexaRequest(intentName, requestType = 'IntentRequest', slots = {}, sessionAttributes = {}) {
  const formattedSlots = {};
  for (const [key, val] of Object.entries(slots)) {
    formattedSlots[key] = {
      name: key,
      value: val,
      confirmationStatus: 'NONE'
    };
  }

  return {
    version: '1.0',
    session: {
      new: Object.keys(sessionAttributes).length === 0,
      sessionId: 'amzn1.echo-api.session.test-session',
      application: { applicationId: 'amzn1.ask.skill.test' },
      attributes: sessionAttributes,
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
        confirmationStatus: 'NONE',
        slots: formattedSlots
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

    if (req.url === '/api/phone/whatsapp' && req.method === 'POST') {
      return res.end(JSON.stringify({ success: true, message: 'WhatsApp message sent' }));
    }

    if (req.url === '/api/phone/sms' && req.method === 'POST') {
      return res.end(JSON.stringify({ success: true, message: 'SMS message sent' }));
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
  assert.match(speech, /Call answered on speakerphone/);
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
  assert.match(speech, /Call rejected/);
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
  assert.match(speech, /Mobile Buddy is online/);
});

test('Alexa Skill - SendWhatsAppIntent captures target and reprompts for message', async () => {
  const { handler } = require('../lambda/index');
  const req = buildAlexaRequest('SendWhatsAppIntent', 'IntentRequest', { target: 'Rahul' });

  const response = await new Promise((resolve, reject) => {
    handler(req, {}, (err, res) => {
      if (err) reject(err); else resolve(res);
    });
  });

  const speech = response.response.outputSpeech.ssml;
  assert.match(speech, /What message would you like to send to Rahul\?/);
  assert.equal(response.sessionAttributes.pendingAction, 'WHATSAPP');
  assert.equal(response.sessionAttributes.pendingTarget, 'Rahul');
});

test('Alexa Skill - DictateMessageIntent sends WhatsApp when pendingAction is WHATSAPP', async () => {
  const { handler } = require('../lambda/index');
  const req = buildAlexaRequest(
    'DictateMessageIntent',
    'IntentRequest',
    { message: 'I am on my way' },
    { pendingAction: 'WHATSAPP', pendingTarget: 'Rahul' }
  );

  const response = await new Promise((resolve, reject) => {
    handler(req, {}, (err, res) => {
      if (err) reject(err); else resolve(res);
    });
  });

  const speech = response.response.outputSpeech.ssml;
  assert.match(speech, /WhatsApp message sent to Rahul\./);
});

test('Alexa Skill - SendSmsIntent captures target and reprompts for message', async () => {
  const { handler } = require('../lambda/index');
  const req = buildAlexaRequest('SendSmsIntent', 'IntentRequest', { target: 'Papa' });

  const response = await new Promise((resolve, reject) => {
    handler(req, {}, (err, res) => {
      if (err) reject(err); else resolve(res);
    });
  });

  const speech = response.response.outputSpeech.ssml;
  assert.match(speech, /What message would you like to text Papa\?/);
  assert.equal(response.sessionAttributes.pendingAction, 'SMS');
  assert.equal(response.sessionAttributes.pendingTarget, 'Papa');
});

test('Alexa Skill - DictateMessageIntent sends SMS when pendingAction is SMS', async () => {
  const { handler } = require('../lambda/index');
  const req = buildAlexaRequest(
    'DictateMessageIntent',
    'IntentRequest',
    { message: 'Call me back' },
    { pendingAction: 'SMS', pendingTarget: 'Papa' }
  );

  const response = await new Promise((resolve, reject) => {
    handler(req, {}, (err, res) => {
      if (err) reject(err); else resolve(res);
    });
  });

  const speech = response.response.outputSpeech.ssml;
  assert.match(speech, /SMS text sent to Papa\./);
});

