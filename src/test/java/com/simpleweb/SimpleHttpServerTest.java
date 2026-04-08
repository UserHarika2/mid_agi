package com.simpleweb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * JUnit 5 Test class for SimpleHttpServer
 * Following Agile testing principles: fast, independent, repeatable, self-validating, and timely
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimpleHttpServerTest {
    private static final Logger LOGGER = Logger.getLogger(SimpleHttpServerTest.class.getName());
    private static SimpleHttpServer server;
    private static int TEST_PORT;
    private static String BASE_URL;
    
    @BeforeAll
    static void setUp() throws IOException {
        // Find an available port dynamically
        TEST_PORT = findAvailablePort();
        BASE_URL = "http://localhost:" + TEST_PORT;
        
        LOGGER.info("🚀 Starting test server on port " + TEST_PORT);
        server = new SimpleHttpServer(TEST_PORT);
        server.start();
        
        // Wait for server to fully start
        waitForServerReady();
    }
    
    @AfterAll
    static void tearDown() {
        LOGGER.info("🧹 Cleaning up test server");
        if (server != null) {
            server.stop();
            // Give time for the port to be released
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Find an available port by trying to bind to a socket
     */
    private static int findAvailablePort() {
        int startPort = 8080;
        int maxAttempts = 50;
        
        for (int port = startPort; port < startPort + maxAttempts; port++) {
            try (ServerSocket socket = new ServerSocket(port)) {
                socket.setReuseAddress(true);
                LOGGER.info("Found available port: " + port);
                return port;
            } catch (IOException e) {
                // Port is in use, try next one
                LOGGER.fine("Port " + port + " is in use, trying next...");
            }
        }
        
        // Fallback to a random high port
        try (ServerSocket socket = new ServerSocket(0)) {
            int randomPort = socket.getLocalPort();
            LOGGER.info("Using random available port: " + randomPort);
            return randomPort;
        } catch (IOException e) {
            throw new RuntimeException("No available ports found", e);
        }
    }
    
    private static void waitForServerReady() {
        int maxRetries = 10;
        int retryDelay = 500; // milliseconds
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                URL url = new URL(BASE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                conn.connect();
                if (conn.getResponseCode() == 200) {
                    LOGGER.info("✅ Server is ready on port " + TEST_PORT);
                    conn.disconnect();
                    return;
                }
                conn.disconnect();
            } catch (Exception e) {
                // Server not ready yet
                LOGGER.fine("Waiting for server to start... (attempt " + (i + 1) + "/" + maxRetries + ")");
            }
            
            try {
                Thread.sleep(retryDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        LOGGER.warning("⚠️ Server may not be fully ready, but continuing tests");
    }
    
    @Test
    @Order(1)
    @DisplayName("Test server is running")
    void testServerIsRunning() {
        assertTrue(server.isRunning(), "Server should be running");
        assertEquals(TEST_PORT, server.getPort(), "Server port should match");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test homepage returns HTTP 200")
    void testHomePageReturns200() throws IOException {
        URL url = new URL(BASE_URL + "/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode, "Homepage should return 200 OK");
        
        connection.disconnect();
    }
    
    @Test
    @Order(3)
    @DisplayName("Test index.html returns HTML content")
    void testIndexHtmlContent() throws IOException {
        URL url = new URL(BASE_URL + "/index.html");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        assertEquals(200, connection.getResponseCode());
        
        String contentType = connection.getContentType();
        assertTrue(contentType.contains("text/html"), "Content type should be HTML");
        
        // Read response body
        String content = readResponse(connection);
        assertNotNull(content);
        assertTrue(content.contains("simpleSpace") || content.contains("simpleSpace"), 
                  "Page should contain 'simpleSpace'");
        assertTrue(content.contains("Craft") || content.contains("digital"), 
                  "Page should contain hero text");
        
        connection.disconnect();
    }
    
    @Test
    @Order(4)
    @DisplayName("Test HTML contains CSS styles")
    void testHtmlContainsStyles() throws IOException {
        URL url = new URL(BASE_URL + "/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        String content = readResponse(connection);
        
        // Check for CSS styles
        assertTrue(content.contains("<style>") || content.contains("background:"), 
                  "HTML should contain CSS styles");
        assertTrue(content.contains("flex") || content.contains("grid"), 
                  "Page should contain modern CSS layout properties");
        
        connection.disconnect();
    }
    
    @Test
    @Order(5)
    @DisplayName("Test navigation links exist")
    void testNavigationLinks() throws IOException {
        URL url = new URL(BASE_URL + "/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        String content = readResponse(connection);
        
        // Check for navigation links
        assertTrue(content.contains("nav-links") || content.contains("nav"), 
                  "Navigation should have nav-links class");
        
        connection.disconnect();
    }
    
    @Test
    @Order(6)
    @DisplayName("Test responsive meta viewport tag")
    void testResponsiveMetaTag() throws IOException {
        URL url = new URL(BASE_URL + "/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        String content = readResponse(connection);
        
        assertTrue(content.contains("viewport"), 
                  "Page should have viewport meta tag for responsive design");
        
        connection.disconnect();
    }
    
    @Test
    @Order(7)
    @DisplayName("Test 404 page for non-existent routes")
    void test404PageForNonExistentRoute() throws IOException {
        URL url = new URL(BASE_URL + "/non-existent-page-12345");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        assertEquals(404, responseCode, "Non-existent page should return 404");
        
        connection.disconnect();
    }
    
    @Test
    @Order(8)
    @DisplayName("Test correct content type headers")
    void testContentTypeHeaders() throws IOException {
        URL url = new URL(BASE_URL + "/index.html");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        String contentType = connection.getContentType();
        assertNotNull(contentType);
        assertTrue(contentType.startsWith("text/html"), "Content-Type should be text/html");
        
        connection.disconnect();
    }
    
    @Test
    @Order(9)
    @DisplayName("Test page contains interactive elements")
    void testPageContainsInteractiveElements() throws IOException {
        URL url = new URL(BASE_URL + "/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        String content = readResponse(connection);
        
        // Check for buttons and interactive elements
        assertTrue(content.contains("btn") || content.contains("button"), 
                  "Page should contain button elements");
        assertTrue(content.contains("Get started") || content.contains("Explore"), 
                  "Page should have call-to-action buttons");
        
        connection.disconnect();
    }
    
    @Test
    @Order(10)
    @DisplayName("Test server response time is under 3 seconds")
    void testServerResponseTime() throws IOException {
        URL url = new URL(BASE_URL + "/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        long startTime = System.currentTimeMillis();
        connection.connect();
        int responseCode = connection.getResponseCode();
        long endTime = System.currentTimeMillis();
        
        long responseTime = endTime - startTime;
        assertEquals(200, responseCode);
        assertTrue(responseTime < 3000, 
                  String.format("Response time should be under 3 seconds, but was %d ms", responseTime));
        
        LOGGER.info(String.format("Response time: %d ms", responseTime));
        connection.disconnect();
    }
    
    @Test
    @Order(11)
    @DisplayName("Test HTML validation - basic structure")
    void testHtmlStructure() throws IOException {
        URL url = new URL(BASE_URL + "/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        String content = readResponse(connection);
        
        // Check for basic HTML structure
        assertTrue(content.contains("<!DOCTYPE html>") || content.contains("<html"), 
                  "Page should have HTML doctype or html tag");
        assertTrue(content.contains("<head>") || content.contains("<title>"), 
                  "Page should have head section");
        assertTrue(content.contains("<body>"), "Page should have body section");
        
        connection.disconnect();
    }
    
    @Test
    @Order(12)
    @DisplayName("Test server serves static files correctly")
    void testServesStaticFiles() throws IOException {
        // Test that the server can serve the HTML file
        URL url = new URL(BASE_URL + "/index.html");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);
        
        String content = readResponse(connection);
        assertTrue(content.length() > 100, "HTML content should be substantial");
        
        connection.disconnect();
    }
    
    private String readResponse(HttpURLConnection connection) throws IOException {
        StringBuilder content = new StringBuilder();
        try (InputStream is = connection.getInputStream();
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}