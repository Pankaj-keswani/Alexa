const Alexa = require('ask-sdk-core');
const https = require('https');
const http = require('http');

const BACKEND_URL = process.env.BACKEND_URL || 'https://alexa-phonebridge.onrender.com';
const ALEXA_SECRET = process.env.ALEXA_CLIENT_SECRET || 'alexa_skill_secret_456';
const TIMEOUT_MS = 6000;

function callBackend(endpoint, method = 'GET', body = null) {
  return new Promise((resolve) => {
    try {
      const fullUrl = `${BACKEND_URL}${endpoint}`;
      const urlObj = new URL(fullUrl);
      const isHttps = urlObj.protocol === 'https:';
      const client = isHttps ? https : http;

      const postData = body ? JSON.stringify(body) : null;
      const options = {
        hostname: urlObj.hostname,
        port: urlObj.port || (isHttps ? 443 : 80),
        path: urlObj.pathname + urlObj.search,
        method: method,
        headers: {
          'Content-Type': 'application/json',
          'x-alexa-secret': ALEXA_SECRET,
          'ngrok-skip-browser-warning': 'true',
          'User-Agent': 'PhoneBridge-Alexa'
        },
        timeout: TIMEOUT_MS
      };

      if (postData) {
        options.headers['Content-Length'] = Buffer.byteLength(postData);
      }

      const req = client.request(options, (res) => {
        let rawData = '';
        res.on('data', (chunk) => { rawData += chunk; });
        res.on('end', () => {
          let data = null;
          try { data = JSON.parse(rawData); } catch (e) {}
          resolve({ ok: res.statusCode >= 200 && res.statusCode < 300, status: res.statusCode, data: data });
        });
      });

      req.on('timeout', () => {
        req.destroy();
        resolve({ ok: false, status: 0, error: 'TIMEOUT' });
      });

      req.on('error', (err) => {
        resolve({ ok: false, status: 0, error: err.message });
      });

      if (postData) {
        req.write(postData);
      }
      req.end();
    } catch (err) {
      resolve({ ok: false, status: 0, error: err.message });
    }
  });
}

function getSlotVal(handlerInput, slotName) {
  try {
    const envelope = handlerInput && handlerInput.requestEnvelope;
    const request = envelope && envelope.request;
    const intent = request && request.intent;
    const slots = intent && intent.slots;
    const slot = slots && slots[slotName];
    if (!slot) return null;

    const resolutions = slot.resolutions && slot.resolutions.resolutionsPerAuthority;
    if (resolutions && resolutions.length > 0) {
      const first = resolutions[0];
      if (first.status && first.status.code === 'ER_SUCCESS_MATCH' && first.values && first.values.length > 0) {
        return first.values[0].value.name;
      }
    }
    return slot.value || null;
  } catch (err) {
    return null;
  }
}

// 1. Launch Request
const LaunchRequestHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'LaunchRequest';
  },
  async handle(handlerInput) {
    const statusRes = await callBackend('/api/call/status', 'GET');

    if (statusRes.ok && statusRes.data && statusRes.data.hasActiveCall && statusRes.data.state === 'RINGING') {
      const caller = statusRes.data.callerName || 'Unknown caller';
      const speechText = `You have an incoming call from ${caller}. Would you like me to answer or reject it?`;
      return handlerInput.responseBuilder
        .speak(speechText)
        .reprompt('Would you like to answer or reject the call?')
        .getResponse();
    }

    const speechText = 'Mobile Buddy is online. You can find your phone, check battery or storage, control flashlight, adjust volume, launch apps, announce a message, or make calls. What can I do for you?';
    return handlerInput.responseBuilder
      .speak(speechText)
      .reprompt('How can I help with your phone?')
      .getResponse();
  }
};

// 2. Incoming Call Management
const GetCallerIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'GetCallerIntent';
  },
  async handle(handlerInput) {
    const statusRes = await callBackend('/api/call/status', 'GET');

    if (!statusRes.ok || !statusRes.data) {
      return handlerInput.responseBuilder
        .speak('I was unable to connect to your phone backend. Please make sure the app and backend are running.')
        .getResponse();
    }

    const { hasActiveCall, callerName, state } = statusRes.data;

    if (!hasActiveCall || state !== 'RINGING') {
      return handlerInput.responseBuilder
        .speak('There is no incoming call right now.')
        .getResponse();
    }

    const caller = callerName || 'Unknown caller';
    return handlerInput.responseBuilder
      .speak(`You have an incoming call from ${caller}.`)
      .getResponse();
  }
};

const AnswerCallIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'AnswerCallIntent';
  },
  async handle(handlerInput) {
    const statusRes = await callBackend('/api/call/status', 'GET');
    if (!statusRes.ok || !statusRes.data || !statusRes.data.hasActiveCall || statusRes.data.state !== 'RINGING') {
      return handlerInput.responseBuilder
        .speak('There is no incoming call to answer.')
        .getResponse();
    }

    const answerRes = await callBackend('/api/call/answer', 'POST', {});

    if (answerRes.ok && answerRes.data && answerRes.data.success) {
      return handlerInput.responseBuilder
        .speak('Call answered on speakerphone.')
        .getResponse();
    } else {
      return handlerInput.responseBuilder
        .speak("I couldn't answer the call. Please check your phone permissions.")
        .getResponse();
    }
  }
};

const RejectCallIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'RejectCallIntent';
  },
  async handle(handlerInput) {
    const statusRes = await callBackend('/api/call/status', 'GET');
    if (!statusRes.ok || !statusRes.data || !statusRes.data.hasActiveCall || statusRes.data.state !== 'RINGING') {
      return handlerInput.responseBuilder
        .speak('There is no incoming call to reject.')
        .getResponse();
    }

    const rejectRes = await callBackend('/api/call/reject', 'POST', {});

    if (rejectRes.ok && rejectRes.data && rejectRes.data.success) {
      return handlerInput.responseBuilder
        .speak('Call rejected.')
        .getResponse();
    } else {
      return handlerInput.responseBuilder
        .speak("I couldn't reject the call.")
        .getResponse();
    }
  }
};

const CallStatusIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'CallStatusIntent';
  },
  async handle(handlerInput) {
    const statusRes = await callBackend('/api/call/status', 'GET');

    if (!statusRes.ok || !statusRes.data) {
      return handlerInput.responseBuilder
        .speak('Could not check call status.')
        .getResponse();
    }

    const { hasActiveCall, state, callerName } = statusRes.data;

    if (!hasActiveCall || state === 'IDLE') {
      return handlerInput.responseBuilder
        .speak('There is no active call on your phone right now.')
        .getResponse();
    }

    const caller = callerName || 'Unknown caller';
    if (state === 'RINGING') {
      return handlerInput.responseBuilder
        .speak(`You have an incoming call from ${caller}.`)
        .getResponse();
    } else if (state === 'ACTIVE') {
      return handlerInput.responseBuilder
        .speak(`You are currently in an active call with ${caller}.`)
        .getResponse();
    }

    return handlerInput.responseBuilder
      .speak('There is no incoming call right now.')
      .getResponse();
  }
};

// 3. Find My Phone (Alarm / Siren)
const FindPhoneIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'FindPhoneIntent';
  },
  async handle(handlerInput) {
    const res = await callBackend('/api/phone/find', 'POST', { action: 'START' });
    if (res.ok && res.data && res.data.success) {
      return handlerInput.responseBuilder
        .speak('Sounding the alarm on your phone now.')
        .getResponse();
    } else {
      const msg = (res.data && res.data.message) || 'Please ensure your phone app is open and connected.';
      return handlerInput.responseBuilder
        .speak(`I couldn't trigger the phone alarm. ${msg}`)
        .getResponse();
    }
  }
};

const StopFindPhoneIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'StopFindPhoneIntent';
  },
  async handle(handlerInput) {
    const res = await callBackend('/api/phone/find', 'POST', { action: 'STOP' });
    if (res.ok && res.data && res.data.success) {
      return handlerInput.responseBuilder
        .speak('Phone alarm stopped.')
        .getResponse();
    } else {
      return handlerInput.responseBuilder
        .speak('I could not stop the phone alarm.')
        .getResponse();
    }
  }
};

// 4. Battery & Power Status
const BatteryStatusIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'BatteryStatusIntent';
  },
  async handle(handlerInput) {
    const res = await callBackend('/api/phone/battery', 'GET');
    if (res.ok && res.data && res.data.success) {
      const level = res.data.batteryLevel;
      const charging = res.data.isCharging;
      let speech = `Your phone battery is at ${level} percent.`;
      if (charging) {
        speech = `Your phone is at ${level} percent and is currently charging.`;
      }
      return handlerInput.responseBuilder
        .speak(speech)
        .getResponse();
    } else {
      const msg = (res.data && res.data.message) || 'Could not query phone battery.';
      return handlerInput.responseBuilder
        .speak(`I was unable to check your battery. ${msg}`)
        .getResponse();
    }
  }
};

// 5. Flashlight Control
const FlashlightIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'FlashlightIntent';
  },
  async handle(handlerInput) {
    const stateSlot = (getSlotVal(handlerInput, 'state') || '').toLowerCase();
    const isOff = stateSlot.indexOf('off') !== -1 || stateSlot.indexOf('disable') !== -1 || stateSlot.indexOf('stop') !== -1;
    const targetState = !isOff;

    const res = await callBackend('/api/phone/flashlight', 'POST', { state: targetState });
    if (res.ok && res.data && res.data.success) {
      const text = targetState ? 'Turned on flashlight.' : 'Turned off flashlight.';
      return handlerInput.responseBuilder
        .speak(text)
        .getResponse();
    } else {
      const msg = (res.data && res.data.message) || 'Please check your phone app.';
      return handlerInput.responseBuilder
        .speak(`I couldn't change the flashlight state. ${msg}`)
        .getResponse();
    }
  }
};

// 6. Make Outgoing Call
const MakeCallIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'MakeCallIntent';
  },
  async handle(handlerInput) {
    const target = getSlotVal(handlerInput, 'target');
    if (!target) {
      return handlerInput.responseBuilder
        .speak('Who would you like me to call?')
        .reprompt('Please tell me the contact name or number to call.')
        .getResponse();
    }

    const res = await callBackend('/api/phone/call', 'POST', { target });
    if (res.ok && res.data && res.data.success) {
      return handlerInput.responseBuilder
        .speak(`Calling ${target} on speakerphone.`)
        .getResponse();
    } else {
      const msg = (res.data && res.data.message) || `I couldn't place the call to ${target}.`;
      return handlerInput.responseBuilder
        .speak(msg)
        .getResponse();
    }
  }
};

// 7. Send WhatsApp Message
const SendWhatsAppIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'SendWhatsAppIntent';
  },
  async handle(handlerInput) {
    const target = getSlotVal(handlerInput, 'target');
    const attributesManager = handlerInput.attributesManager;
    const sessionAttributes = attributesManager.getSessionAttributes() || {};

    sessionAttributes.pendingAction = 'WHATSAPP';

    if (!target) {
      attributesManager.setSessionAttributes(sessionAttributes);
      return handlerInput.responseBuilder
        .speak('Who would you like to send a WhatsApp message to?')
        .reprompt('Please say the contact name you want to message.')
        .getResponse();
    }

    sessionAttributes.pendingTarget = target;
    attributesManager.setSessionAttributes(sessionAttributes);

    const speechText = 'What message would you like to send to ' + target + '?';
    const repromptText = 'Please say the message you want to send to ' + target + '.';
    return handlerInput.responseBuilder
      .speak(speechText)
      .reprompt(repromptText)
      .getResponse();
  }
};

// 8. Open App Launcher
const OpenAppIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'OpenAppIntent';
  },
  async handle(handlerInput) {
    const appName = getSlotVal(handlerInput, 'appName');
    if (!appName) {
      return handlerInput.responseBuilder
        .speak('Which app would you like me to open on your phone?')
        .reprompt('You can say YouTube, Spotify, Camera, Maps, or Settings.')
        .getResponse();
    }

    const res = await callBackend('/api/phone/app', 'POST', { appName });
    if (res.ok && res.data && res.data.success) {
      return handlerInput.responseBuilder
        .speak(`Opening ${appName} on your phone.`)
        .getResponse();
    } else {
      const msg = (res.data && res.data.message) || `Could not open ${appName}.`;
      return handlerInput.responseBuilder
        .speak(msg)
        .getResponse();
    }
  }
};

// 9. Sound Profile & Volume Control
const SetVolumeIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'SetVolumeIntent';
  },
  async handle(handlerInput) {
    const rawMode = (getSlotVal(handlerInput, 'mode') || 'normal').toLowerCase();
    let mode = 'NORMAL';
    if (rawMode.indexOf('silent') !== -1 || rawMode.indexOf('mute') !== -1 || rawMode.indexOf('quiet') !== -1) {
      mode = 'SILENT';
    } else if (rawMode.indexOf('vibrate') !== -1) {
      mode = 'VIBRATE';
    } else if (rawMode.indexOf('max') !== -1 || rawMode.indexOf('loud') !== -1) {
      mode = 'MAX';
    }

    const res = await callBackend('/api/phone/volume', 'POST', { mode });
    if (res.ok && res.data && res.data.success) {
      let text = `Phone sound mode set to ${mode.toLowerCase()}.`;
      if (mode === 'MAX') text = 'Phone volume set to maximum.';
      if (mode === 'SILENT') text = 'Phone muted.';
      return handlerInput.responseBuilder
        .speak(text)
        .getResponse();
    } else {
      const msg = (res.data && res.data.message) || 'Could not adjust phone sound mode.';
      return handlerInput.responseBuilder
        .speak(msg)
        .getResponse();
    }
  }
};

// 10. Voice Broadcast / TTS Intercom
const SpeakMessageIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'SpeakMessageIntent';
  },
  async handle(handlerInput) {
    const message = getSlotVal(handlerInput, 'message');
    if (!message) {
      return handlerInput.responseBuilder
        .speak('What message would you like me to broadcast on your phone?')
        .reprompt('Please say the message you want your phone to speak.')
        .getResponse();
    }

    const res = await callBackend('/api/phone/speak', 'POST', { message });
    if (res.ok && res.data && res.data.success) {
      return handlerInput.responseBuilder
        .speak('Broadcasting message on your phone.')
        .getResponse();
    } else {
      const msg = (res.data && res.data.statusText) || 'Could not broadcast message to phone.';
      return handlerInput.responseBuilder
        .speak(msg)
        .getResponse();
    }
  }
};

// 11. Storage & RAM Device Stats
const DeviceStatsIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'DeviceStatsIntent';
  },
  async handle(handlerInput) {
    const res = await callBackend('/api/phone/stats', 'GET');
    if (res.ok && res.data && res.data.success) {
      const freeStorageGb = res.data.freeStorageGb;
      const totalStorageGb = res.data.totalStorageGb;
      const freeRamGb = res.data.freeRamGb;
      const totalRamGb = res.data.totalRamGb;
      const speech = `Your phone has ${freeStorageGb} gigabytes of free storage out of ${totalStorageGb} gigabytes, and ${freeRamGb} gigabytes of free RAM out of ${totalRamGb} gigabytes.`;
      return handlerInput.responseBuilder
        .speak(speech)
        .getResponse();
    } else {
      const msg = (res.data && res.data.message) || 'Could not query storage details from phone.';
      return handlerInput.responseBuilder
        .speak(msg)
        .getResponse();
    }
  }
};

// 12. Direct SMS (Turn 1: Target capture)
const SendSmsIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'SendSmsIntent';
  },
  async handle(handlerInput) {
    const target = getSlotVal(handlerInput, 'target');
    const attributesManager = handlerInput.attributesManager;
    const sessionAttributes = attributesManager.getSessionAttributes() || {};

    sessionAttributes.pendingAction = 'SMS';

    if (!target) {
      attributesManager.setSessionAttributes(sessionAttributes);
      return handlerInput.responseBuilder
        .speak('Who would you like me to send a text message to?')
        .reprompt('Please say the contact name or phone number.')
        .getResponse();
    }

    sessionAttributes.pendingTarget = target;
    attributesManager.setSessionAttributes(sessionAttributes);

    const speechText = 'What message would you like to text ' + target + '?';
    const repromptText = 'Please say the message you want to text ' + target + '.';
    return handlerInput.responseBuilder
      .speak(speechText)
      .reprompt(repromptText)
      .getResponse();
  }
};

// 13. Dictate Message (Turn 2: Content capture & send for WhatsApp or SMS)
const DictateMessageIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'DictateMessageIntent';
  },
  async handle(handlerInput) {
    const message = getSlotVal(handlerInput, 'message');
    const attributesManager = handlerInput.attributesManager;
    const sessionAttributes = attributesManager.getSessionAttributes() || {};
    const pendingAction = sessionAttributes.pendingAction;
    const pendingTarget = sessionAttributes.pendingTarget;

    if (!message) {
      return handlerInput.responseBuilder
        .speak('I did not catch the message. What would you like to say?')
        .reprompt('Please say the message you would like to send.')
        .getResponse();
    }

    if (!pendingTarget) {
      return handlerInput.responseBuilder
        .speak('Who would you like me to send that message to, and should I send it via WhatsApp or SMS?')
        .reprompt('Please say: send WhatsApp to contact name, or send SMS to contact name.')
        .getResponse();
    }

    // Clear pending session state
    attributesManager.setSessionAttributes({});

    if (pendingAction === 'SMS') {
      const res = await callBackend('/api/phone/sms', 'POST', { target: pendingTarget, message: message });
      if (res.ok && res.data && res.data.success) {
        return handlerInput.responseBuilder
          .speak('SMS text sent to ' + pendingTarget + '.')
          .getResponse();
      } else {
        const msg = (res.data && res.data.message) || ('Could not send SMS to ' + pendingTarget + '.');
        return handlerInput.responseBuilder
          .speak(msg)
          .getResponse();
      }
    } else {
      // Default to WhatsApp
      const res = await callBackend('/api/phone/whatsapp', 'POST', { target: pendingTarget, message: message });
      if (res.ok && res.data && res.data.success) {
        return handlerInput.responseBuilder
          .speak('WhatsApp message sent to ' + pendingTarget + '.')
          .getResponse();
      } else {
        const msg = (res.data && res.data.message) || ("I couldn't send the WhatsApp message to " + pendingTarget + '.');
        return handlerInput.responseBuilder
          .speak(msg)
          .getResponse();
      }
    }
  }
};

// Standard Handlers
const HelpIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'AMAZON.HelpIntent';
  },
  handle(handlerInput) {
    const speechText = 'You can say: find my phone, check battery, check storage, put phone on silent, open YouTube, call Mummy on speaker, announce dinner is ready on phone, or send WhatsApp to Rahul. What would you like to do?';
    return handlerInput.responseBuilder
      .speak(speechText)
      .reprompt(speechText)
      .getResponse();
  }
};

const CancelAndStopIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && (Alexa.getIntentName(handlerInput.requestEnvelope) === 'AMAZON.CancelIntent'
        || Alexa.getIntentName(handlerInput.requestEnvelope) === 'AMAZON.StopIntent');
  },
  handle(handlerInput) {
    return handlerInput.responseBuilder
      .speak('Goodbye!')
      .getResponse();
  }
};

const FallbackIntentHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'IntentRequest'
      && Alexa.getIntentName(handlerInput.requestEnvelope) === 'AMAZON.FallbackIntent';
  },
  handle(handlerInput) {
    const attributesManager = handlerInput.attributesManager;
    const sessionAttributes = attributesManager.getSessionAttributes() || {};
    if (sessionAttributes.pendingTarget) {
      const target = sessionAttributes.pendingTarget;
      const speechText = "I didn't quite catch that. You can say: saying, followed by your message for " + target + ", or say cancel.";
      return handlerInput.responseBuilder
        .speak(speechText)
        .reprompt('What message should I send to ' + target + '?')
        .getResponse();
    }
    const speechText = "I didn't quite catch that. You can ask to find your phone, open an app, check battery or storage, set volume, make a call, or send WhatsApp.";
    return handlerInput.responseBuilder
      .speak(speechText)
      .reprompt(speechText)
      .getResponse();
  }
};

const SessionEndedRequestHandler = {
  canHandle(handlerInput) {
    return Alexa.getRequestType(handlerInput.requestEnvelope) === 'SessionEndedRequest';
  },
  handle(handlerInput) {
    return handlerInput.responseBuilder.getResponse();
  }
};

const ErrorHandler = {
  canHandle() {
    return true;
  },
  handle(handlerInput, error) {
    console.error(`Alexa skill error: ${error.message}`);
    const speechText = 'Sorry, I encountered an error communicating with your phone backend.';
    return handlerInput.responseBuilder
      .speak(speechText)
      .getResponse();
  }
};

exports.handler = Alexa.SkillBuilders.custom()
  .addRequestHandlers(
    LaunchRequestHandler,
    GetCallerIntentHandler,
    AnswerCallIntentHandler,
    RejectCallIntentHandler,
    CallStatusIntentHandler,
    FindPhoneIntentHandler,
    StopFindPhoneIntentHandler,
    BatteryStatusIntentHandler,
    FlashlightIntentHandler,
    MakeCallIntentHandler,
    SendWhatsAppIntentHandler,
    OpenAppIntentHandler,
    SetVolumeIntentHandler,
    SpeakMessageIntentHandler,
    DeviceStatsIntentHandler,
    SendSmsIntentHandler,
    DictateMessageIntentHandler,
    HelpIntentHandler,
    CancelAndStopIntentHandler,
    FallbackIntentHandler,
    SessionEndedRequestHandler
  )
  .addErrorHandlers(ErrorHandler)
  .lambda();
