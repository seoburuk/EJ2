const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  // Regular API proxy
  app.use(
    '/api',
    createProxyMiddleware({
      target: 'http://localhost:8080/ej2',
      changeOrigin: true,
    })
  );

  // WebSocket proxy for chat
  app.use(
    '/ws',
    createProxyMiddleware({
      target: 'http://localhost:8080/ej2',
      changeOrigin: true,
      ws: true, // Enable WebSocket proxying
    })
  );
};
