const express = require('express');
const router = express.Router();
const websocketService = require('../services/websocketService');
const { authenticateAlexaOrDevice } = require('../middleware/authMiddleware');
const logger = require('../utils/logger');

// All phone control routes require Alexa client secret or device token
router.use(authenticateAlexaOrDevice);

/**
 * 1. POST /api/phone/find
 * Triggers Find My Phone siren / strobe on Android
 */
router.post('/find', async (req, res) => {
  const { action = 'START' } = req.body;
  logger.backend(`JARVIS: Find My Phone requested [action=${action}]`);

  const result = await websocketService.sendCommandToDevice('FIND_PHONE', { action });
  res.json({
    success: result.success,
    action,
    message: result.success ? `Phone alarm ${action === 'START' ? 'started' : 'stopped'}` : result.reason
  });
});

/**
 * 2. GET /api/phone/battery
 * Gets real-time battery level and charging status
 */
router.get('/battery', async (req, res) => {
  logger.backend(`JARVIS: Battery status requested`);

  const result = await websocketService.sendCommandToDevice('GET_BATTERY', {}, null, 4000);
  if (result.success && result.data) {
    res.json({
      success: true,
      batteryLevel: result.data.batteryLevel ?? null,
      isCharging: result.data.isCharging ?? false,
      temperature: result.data.temperature ?? null
    });
  } else {
    res.json({
      success: false,
      message: result.reason || 'Could not query battery status from phone.'
    });
  }
});

/**
 * 3. POST /api/phone/flashlight
 * Toggles phone flashlight / torch
 */
router.post('/flashlight', async (req, res) => {
  const { state = true } = req.body;
  logger.backend(`JARVIS: Flashlight command [state=${state}]`);

  const result = await websocketService.sendCommandToDevice('SET_FLASHLIGHT', { state: !!state });
  res.json({
    success: result.success,
    state: !!state,
    message: result.success ? `Flashlight turned ${state ? 'on' : 'off'}` : result.reason
  });
});

/**
 * 4. POST /api/phone/call
 * Initiates an outgoing call
 */
router.post('/call', async (req, res) => {
  const { target } = req.body;
  if (!target) {
    return res.status(400).json({ success: false, message: 'Missing target contact or phone number' });
  }

  logger.backend(`JARVIS: Outgoing call requested for [${target}]`);
  const result = await websocketService.sendCommandToDevice('MAKE_CALL', { target }, null, 5000);
  res.json({
    success: result.success,
    target,
    message: result.success ? `Calling ${target}` : result.reason
  });
});

/**
 * 5. POST /api/phone/whatsapp
 * Sends or opens a WhatsApp message
 */
router.post('/whatsapp', async (req, res) => {
  const { target, message } = req.body;
  if (!target || !message) {
    return res.status(400).json({ success: false, message: 'Missing target or message' });
  }

  logger.backend(`JARVIS: WhatsApp message requested for [${target}]: "${message}"`);
  const result = await websocketService.sendCommandToDevice('SEND_WHATSAPP', { target, message }, null, 5000);
  res.json({
    success: result.success,
    target,
    message: result.success ? `WhatsApp message sent to ${target}` : result.reason
  });
});

/**
 * 6. POST /api/phone/app
 * Launches any app by name on the phone
 */
router.post('/app', async (req, res) => {
  const { appName } = req.body;
  if (!appName) {
    return res.status(400).json({ success: false, message: 'Missing appName' });
  }

  logger.backend(`JARVIS: App launch requested for [${appName}]`);
  const result = await websocketService.sendCommandToDevice('OPEN_APP', { target: appName }, null, 5000);
  res.json({
    success: result.success,
    appName,
    message: result.success ? `Opened ${appName}` : result.reason
  });
});

/**
 * 7. POST /api/phone/volume
 * Sets volume level or sound profile (SILENT, VIBRATE, NORMAL, MAX)
 */
router.post('/volume', async (req, res) => {
  const { mode = 'NORMAL', level = null } = req.body;
  logger.backend(`JARVIS: Volume command [mode=${mode}, level=${level}]`);

  const result = await websocketService.sendCommandToDevice('SET_VOLUME', { mode, level }, null, 4000);
  res.json({
    success: result.success,
    mode,
    level,
    message: result.success ? `Volume set to ${mode}` : result.reason
  });
});

/**
 * 8. POST /api/phone/speak
 * Intercom broadcast / Text-To-Speech through phone loudspeaker
 */
router.post('/speak', async (req, res) => {
  const { message } = req.body;
  if (!message) {
    return res.status(400).json({ success: false, message: 'Missing message to broadcast' });
  }

  logger.backend(`JARVIS: Broadcast TTS message: "${message}"`);
  const result = await websocketService.sendCommandToDevice('SPEAK_MESSAGE', { message }, null, 5000);
  res.json({
    success: result.success,
    message,
    statusText: result.success ? `Announced: "${message}"` : result.reason
  });
});

/**
 * 9. GET /api/phone/stats
 * Queries storage and RAM status
 */
router.get('/stats', async (req, res) => {
  logger.backend(`JARVIS: Device stats requested`);

  const result = await websocketService.sendCommandToDevice('GET_DEVICE_STATS', {}, null, 4000);
  if (result.success && result.data) {
    res.json({
      success: true,
      freeStorageGb: result.data.freeStorageGb,
      totalStorageGb: result.data.totalStorageGb,
      freeRamGb: result.data.freeRamGb,
      totalRamGb: result.data.totalRamGb
    });
  } else {
    res.json({
      success: false,
      message: result.reason || 'Could not query device stats from phone.'
    });
  }
});

/**
 * 10. POST /api/phone/sms
 * Sends direct SMS hands-free
 */
router.post('/sms', async (req, res) => {
  const { target, message } = req.body;
  if (!target || !message) {
    return res.status(400).json({ success: false, message: 'Missing target or message for SMS' });
  }

  logger.backend(`JARVIS: SMS requested for [${target}]: "${message}"`);
  const result = await websocketService.sendCommandToDevice('SEND_SMS', { target, message }, null, 5000);
  res.json({
    success: result.success,
    target,
    message: result.success ? `SMS sent to ${target}` : result.reason
  });
});

module.exports = router;
