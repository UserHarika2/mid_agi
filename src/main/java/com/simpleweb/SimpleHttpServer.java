package com.simpleweb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Simple HTTP Server to serve the HTML website
 * Follows Agile principles: simple, testable, and maintainable
 */
public class SimpleHttpServer {
    private static final Logger LOGGER = Logger.getLogger(SimpleHttpServer.class.getName());
    private static final int DEFAULT_PORT = 8888;
    private static final String WEB_ROOT = "web/";
    
    private HttpServer server;
    private int port;
    private boolean isRunning;
    
    public SimpleHttpServer() {
        this(DEFAULT_PORT);
    }
    
    public SimpleHttpServer(int port) {
        this.port = port;
        this.isRunning = false;
    }
    
    /**
     * Start the HTTP server
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Create context for root and all static files
        server.createContext("/", new RootHandler());
        server.createContext("/index.html", new RootHandler());
        
        // Set executor for better performance
        server.setExecutor(Executors.newCachedThreadPool());
        
        server.start();
        isRunning = true;
        
        LOGGER.info(String.format("✅ Server started successfully on http://localhost:%d", port));
        LOGGER.info("📁 Serving files from directory: " + new File(WEB_ROOT).getAbsolutePath());
        LOGGER.info("🛑 Press Ctrl+C to stop the server");
    }
    
    /**
     * Stop the HTTP server
     */
    public void stop() {
        if (server != null && isRunning) {
            server.stop(0);
            isRunning = false;
            LOGGER.info("🛑 Server stopped");
        }
    }
    
    /**
     * Check if server is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Get server port
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Handler for serving HTML and static files
     */
    private class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            
            // Default to index.html for root path
            if (requestPath.equals("/") || requestPath.isEmpty()) {
                requestPath = "/index.html";
            }
            
            String filePath = WEB_ROOT + requestPath;
            File file = new File(filePath);
            
            try {
                if (file.exists() && !file.isDirectory()) {
                    serveFile(exchange, file);
                } else {
                    serve404(exchange);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error handling request: " + requestPath, e);
                serveError(exchange, 500, "Internal Server Error");
            }
        }
        
        private void serveFile(HttpExchange exchange, File file) throws IOException {
            String mimeType = getMimeType(file.getName());
            byte[] content = readFileContent(file);
            
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.sendResponseHeaders(200, content.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
            
            LOGGER.fine(String.format("Served: %s (%d bytes)", file.getName(), content.length));
        }
        
        private void serve404(HttpExchange exchange) throws IOException {
            String errorPage = """
                <!DOCTYPE html>
                <html>
                <head><title>404 - Page Not Found</title>
                <style>
                    body { font-family: Arial; text-align: center; padding: 50px; background: #f5f5f5; }
                    h1 { color: #e74c3c; }
                    a { color: #1e6f5c; text-decoration: none; }
                </style>
                </head>
                <body>
                    <h1>404 - Page Not Found</h1>
                    <p>The requested page could not be found.</p>
                    <a href="/">← Back to Home</a>
                </body>
                </html>
                """;
            
            byte[] content = errorPage.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(404, content.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        }
        
        private void serveError(HttpExchange exchange, int statusCode, String message) throws IOException {
            String errorPage = String.format("""
                <html><body><h1>Error %d</h1><p>%s</p></body></html>
                """, statusCode, message);
            
            byte[] content = errorPage.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, content.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        }
        
        private byte[] readFileContent(File file) throws IOException {
            try (FileInputStream fis = new FileInputStream(file)) {
                return fis.readAllBytes();
            }
        }
        
        private String getMimeType(String fileName) {
            if (fileName.endsWith(".html")) return "text/html; charset=UTF-8";
            if (fileName.endsWith(".css")) return "text/css";
            if (fileName.endsWith(".js")) return "application/javascript";
            if (fileName.endsWith(".png")) return "image/png";
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
            if (fileName.endsWith(".svg")) return "image/svg+xml";
            return "text/plain";
        }
    }
    
    /**
     * Main method to run the server standalone
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        // Allow port override via command line
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid port number, using default: " + DEFAULT_PORT);
            }
        }
        
        SimpleHttpServer server = new SimpleHttpServer(port);
        
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown hook triggered");
            server.stop();
        }));
        
        try {
            server.start();
            
            // Keep server running
            synchronized (server) {
                server.wait();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start server", e);
            System.exit(1);
        } catch (InterruptedException e) {
            LOGGER.info("Server interrupted");
            server.stop();
        }
    }
}