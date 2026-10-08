const express = require('express');
const { authenticateDevice } = require('../middleware/authMiddleware');
const logger = require('../utils/logger');

const router = express.Router();

router.post('/register', authenticateDevice, (req, res) => {
  const { deviceId, deviceName, clientVersion } = req.body;

  if (!deviceId) {
    return res.status(400).json({
      success: false,
      message: 'Missing deviceId parameter'
    });
  }

  logger.backend(`Device registered: ${deviceName || 'Android'} [${deviceId}] version=${clientVersion || '1.0.0'}`);

  res.json({
    success: true,
    message: 'Device successfully registered',
    data: {
      deviceId,
      registeredAt: new Date().toISOString()
    }
  });
});

module.exports = router;
