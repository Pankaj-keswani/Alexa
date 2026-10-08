const config = require('../config/env');
const logger = require('../utils/logger');

function extractToken(req, headerName = 'x-device-token') {
  const directHeader = req.headers[headerName.toLowerCase()];
  if (directHeader) return directHeader;

  const authHeader = req.headers['authorization'];
  if (authHeader && authHeader.startsWith('Bearer ')) {
    return authHeader.slice(7).trim();
  }
  return null;
}

/**
 * Middleware: Verify request originates from an authorized Android device.
 */
function authenticateDevice(req, res, next) {
  const token = extractToken(req, 'x-device-token');

  if (!token) {
    logger.backend(`Authentication failed: missing device token for ${req.method} ${req.originalUrl}`);
    return res.status(401).json({
      success: false,
      error: 'Unauthorized: Missing x-device-token header or Bearer token'
    });
  }

  if (token !== config.deviceToken) {
    logger.backend(`Authentication failed: invalid device token for ${req.method} ${req.originalUrl}`);
    return res.status(403).json({
      success: false,
      error: 'Forbidden: Invalid device token'
    });
  }

  req.deviceId = req.headers['x-device-id'] || 'unknown-device';
  next();
}

/**
 * Middleware: Verify request originates from authorized Alexa Skill backend or device.
 */
function authenticateAlexaOrDevice(req, res, next) {
  const alexaSecret = req.headers['x-alexa-secret'];
  const deviceToken = extractToken(req, 'x-device-token');
  const authHeader = req.headers['authorization'];
  const bearerToken = authHeader && authHeader.startsWith('Bearer ') ? authHeader.slice(7).trim() : null;

  const isAlexaValid = alexaSecret === config.alexaClientSecret || bearerToken === config.alexaClientSecret;
  const isDeviceValid = deviceToken === config.deviceToken || bearerToken === config.deviceToken;

  if (isAlexaValid || isDeviceValid) {
    req.callerRole = isAlexaValid ? 'ALEXA' : 'DEVICE';
    return next();
  }

  logger.backend(`Authentication failed: invalid Alexa/Device credentials for ${req.method} ${req.originalUrl}`);
  return res.status(401).json({
    success: false,
    error: 'Unauthorized: Provide valid x-alexa-secret or x-device-token'
  });
}

module.exports = {
  authenticateDevice,
  authenticateAlexaOrDevice
};
