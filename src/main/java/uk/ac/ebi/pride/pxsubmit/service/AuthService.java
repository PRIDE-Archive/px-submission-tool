package uk.ac.ebi.pride.pxsubmit.service;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.archive.submission.model.user.ContactDetail;
import uk.ac.ebi.pride.pxsubmit.config.AppConfig;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * Service for PRIDE authentication.
 * Handles user login and credential validation asynchronously.
 *
 * Usage:
 * <pre>
 * AuthService authService = new AuthService();
 * authService.setCredentials("user@example.com", "password");
 *
 * authService.setOnSucceeded(e -> {
 *     ContactDetail contact = authService.getValue();
 *     if (contact != null) {
 *         // Login successful
 *     }
 * });
 *
 * authService.setOnFailed(e -> {
 *     // Handle error
 * });
 *
 * authService.start();
 * </pre>
 */
public class AuthService extends Service<ContactDetail> {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AppConfig config;
    private String username;
    private String password;

    public AuthService() {
        this.config = AppConfig.getInstance();
    }

    /**
     * Set credentials for authentication
     */
    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    protected Task<ContactDetail> createTask() {
        return new AuthTask(username, password);
    }

    /**
     * Authentication task that runs in background
     */
    private class AuthTask extends Task<ContactDetail> {

        private final String taskUsername;
        private final String taskPassword;

        public AuthTask(String username, String password) {
            this.taskUsername = username;
            this.taskPassword = password;
        }

        @Override
        protected ContactDetail call() throws Exception {
            updateMessage("Authenticating...");

            String loginUrl = config.getUserLoginUrl();
            String toolVersion = config.getToolVersion();

            logger.info("Starting authentication for user: {}", taskUsername);
            logger.debug("Login URL: {}", loginUrl);

            try {
                RestTemplate restTemplate = createRestTemplate();

                // Prepare headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.add("version", toolVersion);

                // Prepare credentials payload using a Map for proper JSON serialization
                java.util.Map<String, String> credentials = new java.util.LinkedHashMap<>();
                credentials.put("username", taskUsername);
                credentials.put("password", taskPassword);

                HttpEntity<java.util.Map<String, String>> entity = new HttpEntity<>(credentials, headers);

                updateMessage("Contacting PRIDE server...");

                // Make login request
                ResponseEntity<ContactDetail> response = restTemplate.exchange(
                    loginUrl,
                    HttpMethod.POST,
                    entity,
                    ContactDetail.class
                );

                ContactDetail contactDetail = response.getBody();

                if (contactDetail == null) {
                    logger.error("Login succeeded but returned null ContactDetail");
                    throw new AuthenticationException("Login failed: Invalid server response");
                }

                logger.info("Authentication successful for user: {}", taskUsername);
                updateMessage("Login successful");

                return contactDetail;

            } catch (ResourceAccessException e) {
                logger.error("Network error during authentication: {}", e.getMessage());
                throw new AuthenticationException(
                    "Network error: Unable to connect to PRIDE server. " +
                    "Please check your internet connection and proxy settings.", e);

            } catch (HttpClientErrorException e) {
                logger.error("HTTP error during authentication: {} - {}",
                    e.getStatusCode(), e.getMessage());

                // Extract the server's error message from the response body
                String serverMessage = e.getResponseBodyAsString();
                // Clean up quoted strings like "Invalid Password!" → Invalid Password!
                if (serverMessage != null) {
                    serverMessage = serverMessage.trim();
                    if (serverMessage.startsWith("\"") && serverMessage.endsWith("\"")) {
                        serverMessage = serverMessage.substring(1, serverMessage.length() - 1);
                    }
                    // Strip HTTP status prefix like "400 : " or "404 : "
                    if (serverMessage.matches("^\\d{3}\\s*:\\s*.*")) {
                        serverMessage = serverMessage.replaceFirst("^\\d{3}\\s*:\\s*", "").trim();
                        if (serverMessage.startsWith("\"") && serverMessage.endsWith("\"")) {
                            serverMessage = serverMessage.substring(1, serverMessage.length() - 1);
                        }
                    }
                }

                if (serverMessage != null && !serverMessage.isEmpty()) {
                    throw new AuthenticationException(serverMessage, e);
                } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                           e.getStatusCode() == HttpStatus.FORBIDDEN) {
                    throw new AuthenticationException(
                        "Invalid credentials: Please check your email and password.", e);
                } else {
                    throw new AuthenticationException(
                        "Server error: " + e.getStatusText(), e);
                }

            } catch (AuthenticationException e) {
                throw e;

            } catch (Exception e) {
                logger.error("Unexpected error during authentication: {}", e.getMessage(), e);
                throw new AuthenticationException(
                    "Unexpected error: " + e.getMessage(), e);
            }
        }

        private RestTemplate createRestTemplate() {
            RestTemplate restTemplate = new RestTemplate();

            // Configure proxy if enabled
            if (config.isProxyEnabled()) {
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setProxy(new Proxy(
                    Proxy.Type.HTTP,
                    new InetSocketAddress(config.getProxyHost(), config.getProxyPort())
                ));
                restTemplate.setRequestFactory(factory);
                logger.debug("Proxy configured: {}:{}", config.getProxyHost(), config.getProxyPort());
            }

            return restTemplate;
        }
    }

    /**
     * Custom exception for authentication errors
     */
    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }

        public AuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ==================== Convenience Methods ====================

    /**
     * Check if credentials are set
     */
    public boolean hasCredentials() {
        return username != null && !username.isEmpty() &&
               password != null && !password.isEmpty();
    }

    /**
     * Clear stored credentials
     */
    public void clearCredentials() {
        this.username = null;
        this.password = null;
    }

    public String getUsername() {
        return username;
    }
}
