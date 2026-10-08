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

// Root Status Page
app.get('/', (req, res) => {
  res.send(`
    <!DOCTYPE html>
    <html>
      <head>
        <title>PhoneBridge JARVIS Cloud</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0f172a; color: #f8fafc; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; text-align: center; }
          .card { background: #1e293b; padding: 2.5rem; border-radius: 1rem; box-shadow: 0 10px 25px rgba(0,0,0,0.5); border: 1px solid #334155; max-width: 450px; }
          .status { display: inline-flex; align-items: center; gap: 0.5rem; background: #064e3b; color: #34d399; padding: 0.35rem 1rem; border-radius: 9999px; font-weight: 600; font-size: 0.875rem; margin-bottom: 1rem; }
          .dot { width: 10px; height: 10px; background: #10b981; border-radius: 50%; box-shadow: 0 0 8px #10b981; }
          h1 { margin: 0.5rem 0 1rem; font-size: 1.75rem; }
          p { color: #94a3b8; font-size: 0.95rem; line-height: 1.5; margin: 0.5rem 0; }
          code { background: #0f172a; padding: 0.2rem 0.5rem; border-radius: 4px; color: #38bdf8; }
        </style>
      </head>
      <body>
        <div class="card">
          <div class="status"><span class="dot"></span> Online &amp; Ready</div>
          <h1>PhoneBridge JARVIS</h1>
          <p>Your Alexa Phone Control backend is running 24/7 in the cloud.</p>
          <p>WebSocket: <code>/ws</code> &bull; Health: <code>/api/health</code></p>
        </div>
      </body>
    </html>
  `);
});

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
