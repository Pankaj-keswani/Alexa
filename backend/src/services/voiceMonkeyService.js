const https = require('https');
const logger = require('../utils/logger');

const TOKEN = process.env.VOICEMONKEY_TOKEN || 'a0681-4b3e8-d3085-73ea3-e4ff9-ab2d6-5e-51f35f71-63b0-4bb6-96a6-4b65c73da88d';
const DEVICE = process.env.VOICEMONKEY_DEVICE || 'alexa-echo-dot-5-kw9az';

/**
 * Triggers an immediate proactive voice announcement on Echo Dot.
 * @param {string} callerName - Name of the caller (or masked number)
 */
function announceIncomingCall(callerName) {
  if (!TOKEN || !DEVICE) {
    logger.warn('Voice Monkey token or device not configured.');
    return;
  }

  const name = callerName || 'Unknown caller';
  const speech = `Incoming call from ${name}!`;

  const url = `https://api-v3.voicemonkey.io/announce?token=${encodeURIComponent(TOKEN)}&device=${encodeURIComponent(DEVICE)}&speech=${encodeURIComponent(speech)}`;

  logger.backend(`Sending proactive voice announcement to Echo Dot: "${speech}"`);

  https.get(url, (res) => {
    let raw = '';
    res.on('data', chunk => { raw += chunk; });
    res.on('end', () => {
      try {
        const json = JSON.parse(raw);
        if (json.success) {
          logger.backend(`Echo Dot successfully announced: "${speech}"`);
        } else {
          logger.warn(`Voice Monkey returned error: ${raw}`);
        }
      } catch (_) {
        logger.backend(`Voice Monkey response: ${raw}`);
      }
    });
  }).on('error', (err) => {
    logger.error(`Voice Monkey announcement request failed: ${err.message}`);
  });
}

module.exports = {
  announceIncomingCall
};
