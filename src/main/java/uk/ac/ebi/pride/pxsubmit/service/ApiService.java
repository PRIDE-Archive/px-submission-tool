package uk.ac.ebi.pride.pxsubmit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.archive.submission.model.submission.SubmissionReferenceDetail;
import uk.ac.ebi.pride.pxsubmit.model.Credentials;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
import uk.ac.ebi.pride.pxsubmit.config.AppConfig;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for PRIDE REST API calls.
 * Provides async methods for:
 * - Getting upload details (FTP/Aspera credentials)
 * - Completing submissions
 * - Verifying uploads
 * - Getting submission status
 *
 * Usage:
 * <pre>
 * ApiService api = new ApiService("user@example.com", "password");
 *
 * // Get upload details asynchronously
 * api.getUploadDetails(UploadMethod.FTP)
 *     .thenAccept(detail -> {
 *         // Use upload details
 *     })
 *     .exceptionally(e -> {
 *         // Handle error
 *         return null;
 *     });
 * </pre>
 */
public class ApiService {

    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> TICKET_FIELD_NAMES = Set.of(
        "ticket",
        "ticketid",
        "ticket_id",
        "reference",
        "accession",
        "submissionid",
        "submission_id"
    );

    private final AppConfig config;
    private final String username;
    private final String password;
    private final String basicAuthHeader;
    private final ExecutorService executor;

    public ApiService(String username, String password) {
        this.config = AppConfig.getInstance();
        this.username = username;
        this.password = password != null ? password : "";
        this.basicAuthHeader = createBasicAuthHeader(username, password);
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "ApiService-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Get upload details for the specified method (FTP or Aspera)
     */
    public CompletableFuture<UploadDetail> getUploadDetails(UploadMethod method) {
        return getUploadDetails(method, null);
    }

    /**
     * Check whether Aspera upload is currently available on the submission service.
     */
    public CompletableFuture<Boolean> isAsperaAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            String url = config.getAsperaAvailableUrl();
            logger.info("Checking Aspera availability");
            logger.debug("Using Aspera availability URL: {}", url);

            try {
                RestTemplate restTemplate = createRestTemplate();
                HttpHeaders headers = createHeaders();
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

                boolean available = parseBooleanResponse(response.getBody());
                logger.info("Aspera availability: {}", available);
                return available;

            } catch (Exception e) {
                logger.warn("Failed to check Aspera availability: {}", e.getMessage(), e);
                throw new ApiException("Failed to check Aspera availability: " + e.getMessage(), e);
            }
        }, executor);
    }

    /**
     * Get existing submission tickets for the authenticated user.
     */
    public CompletableFuture<List<String>> getSubmissionTickets() {
        return CompletableFuture.supplyAsync(() -> {
            String url = config.getSubmissionTicketListUrl();
            logger.info("Getting existing submission tickets");
            logger.debug("Using submission ticket URL: {}", url);

            try {
                RestTemplate restTemplate = createRestTemplate();
                HttpHeaders headers = createHeaders();
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

                List<String> tickets = parseSubmissionTickets(response.getBody());
                logger.info("Retrieved {} existing submission ticket(s)", tickets.size());
                return tickets;

            } catch (Exception e) {
                logger.warn("Failed to get existing submission tickets: {}", e.getMessage(), e);
                throw new ApiException("Failed to get existing submission tickets: " + e.getMessage(), e);
            }
        }, executor);
    }

    /**
     * Get upload details, optionally using an existing ticket for resume scenarios
     */
    public CompletableFuture<UploadDetail> getUploadDetails(UploadMethod method, String ticketId) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Getting upload details for method: {}", method.getMethod());

            RestTemplate restTemplate = createRestTemplate();
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // If ticketId provided, try reupload endpoint first (for resume scenarios)
            if (ticketId != null && !ticketId.isEmpty()) {
                String reuploadUrl = config.getReuploadDetailUrl(method.getMethod(), ticketId);
                logger.debug("Trying reupload URL: {}", reuploadUrl);
                try {
                    ResponseEntity<UploadDetail> response = restTemplate.exchange(
                        reuploadUrl, HttpMethod.GET, entity, UploadDetail.class);
                    UploadDetail detail = response.getBody();
                    if (detail != null && detail.getDropBox() != null) {
                        logger.info("Reupload details retrieved successfully");
                        return detail;
                    }
                    logger.warn("Reupload endpoint returned incomplete details (missing dropBox), falling back to new upload");
                } catch (Exception e) {
                    logger.warn("Reupload endpoint failed, falling back to new upload: {}", e.getMessage());
                }
            }

            // Regular upload endpoint (or fallback from reupload)
            String url = config.getUploadDetailUrl(method.getMethod());
            logger.debug("Using upload URL: {}", url);

            try {
                ResponseEntity<UploadDetail> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, UploadDetail.class);

                UploadDetail detail = response.getBody();
                if (detail == null || detail.getDropBox() == null) {
                    throw new ApiException("Invalid upload details received from server");
                }

                logger.info("Upload details retrieved successfully");
                return detail;

            } catch (Exception e) {
                logger.error("Failed to get upload details: {}", e.getMessage(), e);
                throw new ApiException("Failed to get upload details: " + e.getMessage(), e);
            }
        }, executor);
    }

    /**
     * Complete submission by posting upload details to the submission WS.
     * Returns a SubmissionReferenceDetail containing the ticket/reference ID.
     */
    public CompletableFuture<SubmissionReferenceDetail> completeSubmission(UploadDetail uploadDetail) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Completing submission...");

            String url = config.getSubmissionCompleteUrl();

            try {
                RestTemplate restTemplate = createRestTemplate();
                HttpHeaders headers = createHeaders();
                HttpEntity<UploadDetail> entity = new HttpEntity<>(uploadDetail, headers);

                ResponseEntity<SubmissionReferenceDetail> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, SubmissionReferenceDetail.class);

                SubmissionReferenceDetail result = response.getBody();
                if (result == null) {
                    throw new ApiException("Server returned null response for submission completion");
                }

                logger.info("Submission completed successfully. Reference: {}", result.getReference());
                return result;

            } catch (Exception e) {
                logger.error("Failed to complete submission: {}", e.getMessage(), e);
                throw new ApiException("Failed to complete submission: " + e.getMessage(), e);
            }
        }, executor);
    }

    /**
     * Verify uploaded files
     */
    public CompletableFuture<String> verifyUpload(String folderName) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Verifying upload for folder: {}", folderName);

            String url = config.getUploadVerifyUrl() + "/" + folderName;

            try {
                RestTemplate restTemplate = createRestTemplate();
                HttpHeaders headers = createHeaders();
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

                String result = response.getBody();
                logger.info("Upload verification completed");
                return result;

            } catch (Exception e) {
                logger.error("Failed to verify upload: {}", e.getMessage(), e);
                throw new ApiException("Failed to verify upload: " + e.getMessage(), e);
            }
        }, executor);
    }

    /**
     * Create a JavaFX Service for getting upload details (for UI binding)
     */
    public Service<UploadDetail> createUploadDetailService(UploadMethod method) {
        return new Service<>() {
            @Override
            protected Task<UploadDetail> createTask() {
                return new Task<>() {
                    @Override
                    protected UploadDetail call() throws Exception {
                        updateMessage("Getting upload credentials...");
                        return getUploadDetails(method).get();
                    }
                };
            }
        };
    }

    // ==================== Helper Methods ====================

    private RestTemplate createRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(60000);

        if (config.isProxyEnabled()) {
            factory.setProxy(new Proxy(
                Proxy.Type.HTTP,
                new InetSocketAddress(config.getProxyHost(), config.getProxyPort())
            ));
        }

        restTemplate.setRequestFactory(factory);
        return restTemplate;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        headers.set("Authorization", basicAuthHeader);
        headers.add("version", config.getToolVersion());
        return headers;
    }

    private static String createBasicAuthHeader(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    private static boolean parseBooleanResponse(String body) {
        if (body == null) {
            return false;
        }

        String normalized = body.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return Boolean.parseBoolean(normalized);
    }

    private static List<String> parseSubmissionTickets(String body) {
        if (body == null || body.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalized = body.trim();
        LinkedHashSet<String> tickets = new LinkedHashSet<>();

        try {
            JsonNode root = OBJECT_MAPPER.readTree(normalized);
            collectTicketValues(root, tickets);
        } catch (JsonProcessingException e) {
            addTicketValue(normalized, tickets);
        }

        return new ArrayList<>(tickets);
    }

    private static void collectTicketValues(JsonNode node, Set<String> tickets) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isTextual() || node.isNumber()) {
            addTicketValue(node.asText(), tickets);
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                collectTicketValues(child, tickets);
            }
            return;
        }

        if (!node.isObject()) {
            return;
        }

        node.fields().forEachRemaining(entry -> {
            String normalizedName = entry.getKey()
                .replace("-", "")
                .replace("_", "")
                .toLowerCase(Locale.ROOT);
            JsonNode value = entry.getValue();

            if (TICKET_FIELD_NAMES.contains(normalizedName) && (value.isTextual() || value.isNumber())) {
                addTicketValue(value.asText(), tickets);
            } else if (value.isArray() || value.isObject()) {
                collectTicketValues(value, tickets);
            }
        });
    }

    private static void addTicketValue(String value, Set<String> tickets) {
        if (value == null) {
            return;
        }

        String ticket = value.trim();
        if (!ticket.isEmpty()) {
            tickets.add(ticket);
        }
    }

    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Custom exception for API errors
     */
    public static class ApiException extends RuntimeException {
        public ApiException(String message) {
            super(message);
        }

        public ApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
