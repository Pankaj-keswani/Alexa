const express = require('express');
const { authenticateAlexaOrDevice } = require('../middleware/authMiddleware');
const callStateManager = require('../services/callStateManager');
const websocketService = require('../services/websocketService');
const logger = require('../utils/logger');

const router = express.Router();

/**
 * GET /api/call/status
 * Returns current call status for Alexa or Android app.
 */
router.get('/status', authenticateAlexaOrDevice, (req, res) => {
  const status = callStateManager.getCallStatus();
  const connectedDevices = websocketService.getConnectedDeviceCount();
  logger.alexa(`Call status queried by ${req.callerRole}: state=${status.state}, caller=${status.callerName || 'None'}, connectedDevices=${connectedDevices}`);

  res.json({
    success: true,
    hasActiveCall: status.hasActiveCall,
    state: status.state,
    callerName: status.callerName,
    phoneNumber: status.phoneNumber,
    timestamp: status.updatedAt,
    connectedDevices: connectedDevices
  });
});

/**
 * POST /api/call/answer
 * Triggered by Alexa: "Alexa, ask Phone Bridge to answer"
 * Dispatches ANSWER_CALL command to Android phone over WebSocket.
 */
router.post('/answer', authenticateAlexaOrDevice, async (req, res) => {
  logger.alexa('Answer command received from Alexa');

  const currentStatus = callStateManager.getCallStatus();
  if (!currentStatus.hasActiveCall || currentStatus.state !== 'RINGING') {
    logger.backend('No ringing incoming call found to answer');
    return res.json({
      success: false,
      reason: 'NO_INCOMING_CALL',
      message: 'There is no incoming call to answer.'
    });
  }

  try {
    const result = await websocketService.sendCommandToDevice('ANSWER_CALL', {
      callerName: currentStatus.callerName,
      phoneNumber: currentStatus.phoneNumber
    }, req.body.deviceId);

    logger.backend(`Device answer command response: ${JSON.stringify(result)}`);

    if (result.success) {
      callStateManager.setCallAnsweredLocally();
      return res.json({
        success: true,
        message: 'Call answered successfully.',
        details: result
      });
    } else {
      return res.json({
        success: false,
        reason: result.reason || 'ANDROID_RESTRICTION',
        message: result.reason || 'Could not answer the call due to Android permissions or role requirements.'
      });
    }
  } catch (err) {
    logger.error('BACKEND', 'Error executing answer command', err);
    return res.status(500).json({
      success: false,
      reason: 'SERVER_ERROR',
      message: 'Failed to communicate with Android device.'
    });
  }
});

/**
 * POST /api/call/reject
 * Triggered by Alexa: "Alexa, ask Phone Bridge to reject"
 * Dispatches REJECT_CALL command to Android phone over WebSocket.
 */
router.post('/reject', authenticateAlexaOrDevice, async (req, res) => {
  logger.alexa('Reject command received from Alexa');

  const currentStatus = callStateManager.getCallStatus();
  if (!currentStatus.hasActiveCall || currentStatus.state !== 'RINGING') {
    logger.backend('No ringing incoming call found to reject');
    return res.json({
      success: false,
      reason: 'NO_INCOMING_CALL',
      message: 'There is no incoming call to reject.'
    });
  }

  try {
    const result = await websocketService.sendCommandToDevice('REJECT_CALL', {
      callerName: currentStatus.callerName,
      phoneNumber: currentStatus.phoneNumber
    }, req.body.deviceId);

    logger.backend(`Device reject command response: ${JSON.stringify(result)}`);

    if (result.success) {
      callStateManager.handleCallEvent({
        deviceId: currentStatus.deviceId,
        event: 'CALL_ENDED'
      });
      return res.json({
        success: true,
        message: 'Call rejected successfully.',
        details: result
      });
    } else {
      return res.json({
        success: false,
        reason: result.reason || 'ANDROID_RESTRICTION',
        message: result.reason || 'Could not reject the call because Android did not allow the operation.'
      });
    }
  } catch (err) {
    logger.error('BACKEND', 'Error executing reject command', err);
    return res.status(500).json({
      success: false,
      reason: 'SERVER_ERROR',
      message: 'Failed to communicate with Android device.'
    });
  }
});

module.exports = router;
