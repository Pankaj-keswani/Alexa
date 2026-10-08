const http = require('http');
const express = require('express');
const cors = require('cors');
const config = require('./config/env');
const logger = require('./utils/logger');
const rateLimiter = require('./middleware/rateLimiter');
const websocketService = require('./services/websocketService');

// Route imports
const healthRoutes = require('./routes/healthRoutes');
const deviceRoutes = require('./routes/deviceRoutes');
const eventRoutes = require('./routes/eventRoutes');
const callRoutes = require('./routes/callRoutes');
const phoneRoutes = require('./routes/phoneRoutes');

const app = express();

// Global Middleware
app.use(cors());
app.use(express.json());
app.use(rateLimiter);

// Request logger
app.use((req, res, next) => {
  logger.backend(`${req.method} ${req.originalUrl}`);
  next();
});

// API Routes
app.use('/api/health', healthRoutes);
app.use('/api/device', deviceRoutes);
app.use('/api/events', eventRoutes);
app.use('/api/call', callRoutes);
app.use('/api/phone', phoneRoutes);

// Direct APK download route for mobile installation
app.get('/download/app.apk', (req, res) => {
  const path = require('path');
  const apkPath = path.resolve(__dirname, '../../android/app/build/outputs/apk/debug/app-debug.apk');
  res.download(apkPath, 'PhoneBridge-Jarvis.apk');
});

// Error Handling Middleware
app.use((err, req, res, next) => {
  logger.error('BACKEND', 'Unhandled server error', err);
  res.status(500).json({
    success: false,
    error: 'Internal Server Error',
    message: err.message
  });
});

// HTTP & WebSocket Server Creation
const server = http.createServer(app);
websocketService.initialize(server);

// Start server if executed directly
if (require.main === module) {
  server.listen(config.port, config.host, () => {
    logger.backend(`=============================================`);
    logger.backend(`PhoneBridge Backend running on http://${config.host}:${config.port}`);
    logger.backend(`WebSocket endpoint active on ws://${config.host}:${config.port}/ws`);
    logger.backend(`Health check: http://${config.host}:${config.port}/api/health`);
    logger.backend(`=============================================`);
  });
}

// Graceful shutdown
process.on('SIGTERM', () => {
  logger.backend('SIGTERM received. Shutting down gracefully...');
  websocketService.cleanup();
  server.close(() => process.exit(0));
});

process.on('SIGINT', () => {
  logger.backend('SIGINT received. Shutting down gracefully...');
  websocketService.cleanup();
  server.close(() => process.exit(0));
});

module.exports = { app, server };
