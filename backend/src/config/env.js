require('dotenv').config();

const config = {
  port: parseInt(process.env.PORT, 10) || 3000,
  host: process.env.HOST || '0.0.0.0',
  deviceToken: process.env.DEVICE_TOKEN || 'dev_token_secret_123',
  alexaClientSecret: process.env.ALEXA_CLIENT_SECRET || 'alexa_skill_secret_456',
  defaultDeviceId: process.env.DEFAULT_DEVICE_ID || null,
  nodeEnv: process.env.NODE_ENV || 'development',
};

if (!process.env.DEVICE_TOKEN) {
  console.warn('[WARN] DEVICE_TOKEN not specified in environment. Using default fallback for development.');
}

if (!process.env.ALEXA_CLIENT_SECRET) {
  console.warn('[WARN] ALEXA_CLIENT_SECRET not specified in environment. Using default fallback for development.');
}

module.exports = config;
