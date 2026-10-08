const express = require('express');
const websocketService = require('../services/websocketService');

const router = express.Router();

router.get('/', (req, res) => {
  res.json({
    status: 'ok',
    service: 'PhoneBridge Backend',
    uptime: Math.floor(process.uptime()),
    timestamp: new Date().toISOString(),
    connectedDevices: websocketService.getConnectedDeviceCount()
  });
});

module.exports = router;
