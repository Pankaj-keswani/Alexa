const { WebSocketServer } = require('ws');
const { v4: uuidv4 } = require('uuid');
const config = require('../config/env');
const logger = require('../utils/logger');
const callStateManager = require('./callStateManager');

class WebSocketService {
  constructor() {
    this.wss = null;
    this.connectedClients = new Map(); // deviceId -> { ws, isAlive }
    this.pendingRequests = new Map();  // requestId -> { resolve, reject, timer }
  }

  initialize(server) {
    this.wss = new WebSocketServer({ server, path: '/ws' });

    this.wss.on('connection', (ws, req) => {
      this.handleConnection(ws, req);
    });

    // Heartbeat check interval
    this.heartbeatInterval = setInterval(() => {
      this.wss.clients.forEach((ws) => {
        if (!ws.isAlive) {
          logger.backend('Terminating inactive WebSocket client');
          return ws.terminate();
        }
        ws.isAlive = false;
        ws.ping();
      });
    }, 30000);
    if (this.heartbeatInterval.unref) {
      this.heartbeatInterval.unref();
    }

    logger.backend('WebSocket Server initialized on /ws');
  }

  handleConnection(ws, req) {
    const url = new URL(req.url, 'http://localhost');
    const token = url.searchParams.get('token') || req.headers['x-device-token'];
    const deviceId = url.searchParams.get('deviceId') || req.headers['x-device-id'] || 'device-unknown';

    // Verify token
    if (token !== config.deviceToken) {
      logger.backend(`Rejected WebSocket connection from device [${deviceId}]: invalid token`);
      ws.close(4001, 'Unauthorized');
      return;
    }

    ws.isAlive = true;
    ws.deviceId = deviceId;
    this.connectedClients.set(deviceId, ws);
    logger.backend(`Device [${deviceId}] connected via WebSocket`);

    // Greet and send ping
    ws.send(JSON.stringify({
      type: 'CONNECTION_ACK',
      deviceId,
      message: 'Connected to PhoneBridge backend'
    }));

    ws.on('pong', () => {
      ws.isAlive = true;
    });

    ws.on('message', (data) => {
      try {
        const message = JSON.parse(data.toString());
        this.handleMessage(deviceId, message);
      } catch (err) {
        logger.error('BACKEND', 'Failed to parse incoming WebSocket message', err);
      }
    });

    ws.on('close', (code, reason) => {
      logger.backend(`Device [${deviceId}] disconnected (${code}: ${reason || 'Closed'})`);
      this.connectedClients.delete(deviceId);
    });

    ws.on('error', (err) => {
      logger.error('BACKEND', `WebSocket error for device [${deviceId}]`, err);
    });
  }

  handleMessage(deviceId, message) {
    logger.android(`Incoming WS message from [${deviceId}]: ${JSON.stringify(message)}`);

    // Handle command results from Android
    if (message.requestId && this.pendingRequests.has(message.requestId)) {
      const pending = this.pendingRequests.get(message.requestId);
      clearTimeout(pending.timer);
      this.pendingRequests.delete(message.requestId);
      pending.resolve(message);
      return;
    }

    // Handle call state events forwarded via WS
    if (message.event) {
      callStateManager.handleCallEvent({
        deviceId,
        event: message.event,
        callerName: message.callerName,
        phoneNumber: message.phoneNumber,
        timestamp: message.timestamp
      });
    }
  }

  /**
   * Sends a command (ANSWER_CALL, REJECT_CALL, etc.) to the Android device.
   * Returns a promise that resolves when the Android device acknowledges the command.
   */
  sendCommandToDevice(command, payload = {}, targetDeviceId = null, timeoutMs = 6000) {
    return new Promise((resolve, reject) => {
      // Pick target device
      const deviceId = targetDeviceId || config.defaultDeviceId || Array.from(this.connectedClients.keys())[0];

      if (!deviceId || !this.connectedClients.has(deviceId)) {
        logger.backend(`Cannot send command [${command}]: No connected Android device available`);
        return resolve({
          success: false,
          reason: 'Device unreachable.'
        });
      }

      const client = this.connectedClients.get(deviceId);
      const requestId = uuidv4();

      const message = {
        command,
        requestId,
        ...payload
      };

      const timer = setTimeout(() => {
        if (this.pendingRequests.has(requestId)) {
          this.pendingRequests.delete(requestId);
          logger.backend(`Command [${command}] timed out after ${timeoutMs}ms waiting for device [${deviceId}]`);
          resolve({
            success: false,
            reason: 'Phone response timed out. The device may be sleeping or unreachable.'
          });
        }
      }, timeoutMs);

      this.pendingRequests.set(requestId, { resolve, reject, timer });

      logger.backend(`Command sent to device [${deviceId}]: ${command} (requestId: ${requestId})`);
      client.send(JSON.stringify(message));
    });
  }

  getConnectedDeviceCount() {
    return this.connectedClients.size;
  }

  cleanup() {
    if (this.heartbeatInterval) clearInterval(this.heartbeatInterval);
    if (this.wss) this.wss.close();
  }
}

const websocketService = new WebSocketService();
module.exports = websocketService;
