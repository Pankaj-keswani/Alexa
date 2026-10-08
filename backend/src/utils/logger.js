/**
 * Masks a phone number for privacy in logs.
 * Example: +919876543210 -> +91******3210
 */
function maskPhoneNumber(phone) {
  if (!phone || typeof phone !== 'string') return phone;
  const clean = phone.trim();
  if (clean.length <= 4) return '****';
  const prefix = clean.slice(0, 3);
  const suffix = clean.slice(-4);
  const maskedMiddle = '*'.repeat(Math.max(2, clean.length - 7));
  return `${prefix}${maskedMiddle}${suffix}`;
}

function formatLog(tag, message) {
  const time = new Date().toISOString().replace('T', ' ').slice(0, 23);
  return `[${time}] [${tag}] ${message}`;
}

const logger = {
  android(msg) {
    console.log(formatLog('ANDROID', msg));
  },
  backend(msg) {
    console.log(formatLog('BACKEND', msg));
  },
  alexa(msg) {
    console.log(formatLog('ALEXA', msg));
  },
  telecom(msg) {
    console.log(formatLog('TELECOM', msg));
  },
  error(tag, msg, err) {
    console.error(formatLog(tag, `${msg} ${err ? (err.message || err) : ''}`));
  },
  maskPhoneNumber
};

module.exports = logger;
