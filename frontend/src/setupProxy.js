const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  // Regular API proxy
  app.use(
    '/api',
    createProxyMiddleware({
      target: 'http://localhost:8080',
      changeOrigin: true,
      ws: true, // Enable WebSocket proxy
    })
  );
};
