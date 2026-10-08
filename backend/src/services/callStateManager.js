const logger = require('../utils/logger');
const voiceMonkey = require('./voiceMonkeyService');

class CallStateManager {
  constructor() {
    this.state = {
      hasActiveCall: false,
      state: 'IDLE', // IDLE, RINGING, ACTIVE, DISCONNECTED
      callerName: null,
      phoneNumber: null,
      deviceId: null,
      updatedAt: new Date().toISOString()
    };
  }

  getCallStatus() {
    return { ...this.state };
  }

  handleCallEvent(eventPayload) {
    const { deviceId, event, callerName, phoneNumber, timestamp } = eventPayload;
    const now = timestamp || new Date().toISOString();

    logger.android(`Call Event received from device [${deviceId}]: ${event}`);

    switch (event) {
      case 'INCOMING_CALL':
        this.state = {
          hasActiveCall: true,
          state: 'RINGING',
          callerName: callerName || 'Unknown caller',
          phoneNumber: phoneNumber || null,
          deviceId: deviceId,
          updatedAt: now
        };
        logger.backend(`Call state updated to RINGING. Caller: ${this.state.callerName} (${logger.maskPhoneNumber(this.state.phoneNumber)})`);
        // Proactive Alexa Voice Announcement on Echo Dot
        voiceMonkey.announceIncomingCall(this.state.callerName);
        break;

      case 'CALL_ANSWERED':
        this.state = {
          ...this.state,
          hasActiveCall: true,
          state: 'ACTIVE',
          updatedAt: now
        };
        logger.backend(`Call state updated to ACTIVE.`);
        break;

      case 'CALL_ENDED':
        const previousCaller = this.state.callerName;
        this.state = {
          hasActiveCall: false,
          state: 'IDLE',
          callerName: null,
          phoneNumber: null,
          deviceId: null,
          updatedAt: now
        };
        logger.backend(`Call ended. State reset to IDLE. (Previous: ${previousCaller || 'None'})`);
        break;

      default:
        logger.backend(`Unhandled call event type: ${event}`);
        break;
    }

    return this.getCallStatus();
  }

  setCallAnsweredLocally() {
    this.state.state = 'ACTIVE';
    this.state.updatedAt = new Date().toISOString();
  }

  reset() {
    this.state = {
      hasActiveCall: false,
      state: 'IDLE',
      callerName: null,
      phoneNumber: null,
      deviceId: null,
      updatedAt: new Date().toISOString()
    };
  }
}

const callStateManager = new CallStateManager();
module.exports = callStateManager;
