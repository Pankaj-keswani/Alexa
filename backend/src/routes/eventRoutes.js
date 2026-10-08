const express = require('express');
const { authenticateDevice } = require('../middleware/authMiddleware');
const callStateManager = require('../services/callStateManager');
const logger = require('../utils/logger');

const router = express.Router();

/**
 * POST /api/events/call
 * Handles incoming call events from Android:
 * {
 *   "deviceId": "...",
 *   "event": "INCOMING_CALL" | "CALL_ANSWERED" | "CALL_ENDED",
 *   "callerName": "Rahul",
 *   "phoneNumber": "+91XXXXXXXXXX",
 *   "timestamp": "..."
 * }
 */
router.post('/call', authenticateDevice, (req, res) => {
  const { deviceId, event, callerName, phoneNumber, timestamp } = req.body;

  if (!event) {
    return res.status(400).json({
      success: false,
      message: 'Missing "event" field in payload'
    });
  }

  const updatedState = callStateManager.handleCallEvent({
    deviceId: deviceId || req.deviceId,
    event,
    callerName,
    phoneNumber,
    timestamp
  });

  res.json({
    success: true,
    message: `Event ${event} recorded successfully`,
    currentState: updatedState
  });
});

module.exports = router;
