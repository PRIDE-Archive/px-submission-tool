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
import java.util.Locale;

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
 *     AuthService.AuthenticationResult result = authService.getValue();
 *     if (result != null) {
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
public class AuthService extends Service<AuthService.AuthenticationResult> {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    static final String PENDING_INCOMING_TICKET_MESSAGE =
        "You have a pending unprocessed submission. Please wait until your previous submission gets processed. "
            + "Some tickets might take 1-3 days to process.";

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
    protected Task<AuthenticationResult> createTask() {
        return new AuthTask(username, password);
    }

    /**
     * Authentication task that runs in background
     */
    private class AuthTask extends Task<AuthenticationResult> {

        private final String taskUsername;
        private final String taskPassword;

        public AuthTask(String username, String password) {
            this.taskUsername = username;
            this.taskPassword = password;
        }

        @Override
        protected AuthenticationResult call() throws Exception {
            updateMessage("Authenticating...");

            String loginUrl = config.getUserLoginUrl();

            logger.debug("Starting authentication for user: {}", taskUsername);
            logger.debug("Login URL: {}", loginUrl);

            try {
                RestTemplate restTemplate = createRestTemplate();

                // Prepare headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.add("version", config.getToolVersion());

                // Prepare credentials payload using a Map for proper JSON serialization
                java.util.Map<String, String> credentials = new java.util.LinkedHashMap<>();
                credentials.put("username", taskUsername);
                credentials.put("password", taskPassword);
                logger.info("PRIDE user login request payload: {}", redactPassword(credentials));

                HttpEntity<java.util.Map<String, String>> entity = new HttpEntity<>(credentials, headers);

                updateMessage("Contacting PRIDE server...");


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

                logger.debug("Authentication successful for user: {}", taskUsername);
                updateMessage("Login successful");

                return new AuthenticationResult(null, null, contactDetail);

            } catch (ResourceAccessException e) {
                logger.error("Network error during authentication: {}", e.getMessage());
                throw new AuthenticationException(
                    "Network error: Unable to connect to PRIDE server. " +
                    "Please check your internet connection and proxy settings.", e);

            } catch (HttpClientErrorException e) {
                logger.error("HTTP error during authentication: {} - {}",
                    e.getStatusCode(), e.getMessage());

                // Extract the server's error message from the response body
                String serverMessage = cleanServerMessage(firstNonBlank(
                    e.getResponseBodyAsString(),
                    e.getMessage()
                ));

                if (isPendingIncomingTicketMessage(serverMessage)) {
                    logger.warn("Login blocked for user {} because an incoming ticket is still pending: {}",
                        taskUsername, serverMessage);
                    throw new AuthenticationException(PENDING_INCOMING_TICKET_MESSAGE, e);
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

    private static java.util.Map<String, String> redactPassword(java.util.Map<String, String> payload) {
        java.util.Map<String, String> safePayload = new java.util.LinkedHashMap<>(payload);
        if (safePayload.containsKey("password")) {
            safePayload.put("password", "****");
        }
        return safePayload;
    }

    static boolean isPendingIncomingTicketMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = message.toLowerCase(Locale.ROOT)
            .replace('-', ' ')
            .replace('_', ' ');
        String compact = normalized.replaceAll("[^a-z0-9]", "");

        if (compact.contains("nopendingincomingticket")
            || compact.contains("noincomingticketpending")
            || compact.contains("nopendingunprocessedsubmission")) {
            return false;
        }

        return normalized.contains("incoming ticket pending")
            || normalized.contains("pending incoming ticket")
            || compact.contains("pendingincomingticket")
            || compact.contains("incomingticketpending")
            || compact.contains("pendingunprocessedsubmission")
            || compact.contains("alreadyinprogressticket")
            || compact.contains("alreadyaninprogressticket")
            || compact.contains("alreadyhaveaninprogressticket")
            || (compact.contains("already") && compact.contains("inprogressticket"));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String cleanServerMessage(String message) {
        if (message == null) {
            return null;
        }

        String cleaned = message.trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (cleaned.matches("^\\d{3}\\s*:\\s*.*")) {
            cleaned = cleaned.replaceFirst("^\\d{3}\\s*:\\s*", "").trim();
            if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
            }
        }
        return cleaned;
    }

    public static class AuthenticationResult {
        private final String token;
        private final String info;
        private final ContactDetail contactDetail;

        public AuthenticationResult(String token, String info, ContactDetail contactDetail) {
            this.token = token;
            this.info = info;
            this.contactDetail = contactDetail;
        }

        public String getToken() {
            return token;
        }

        public String getInfo() {
            return info;
        }

        public ContactDetail getContactDetail() {
            return contactDetail;
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
