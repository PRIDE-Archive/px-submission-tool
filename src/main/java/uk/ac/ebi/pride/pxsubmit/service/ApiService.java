package uk.ac.ebi.pride.pxsubmit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.pxsubmit.config.AppConfig;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Base64;
import java.util.Collections;
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

    private final AppConfig config;
    private final String username;
    private final String password;
    private final String basicAuthHeader;
    private final ExecutorService executor;
    private final ObjectMapper objectMapper;

    public ApiService(String username, String password) {
        this.config = AppConfig.getInstance();
        this.username = username;
        this.password = password;
        this.basicAuthHeader = createBasicAuthHeader(username, password);
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "ApiService-Worker");
            t.setDaemon(true);
            return t;
        });
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get upload details for the specified method (FTP or Aspera)
     */
    public CompletableFuture<UploadDetail> getUploadDetails(UploadMethod method) {
        return getUploadDetails(method, null);
    }

    /**
     * Get upload details for resubmission
     */
    public CompletableFuture<UploadDetail> getUploadDetails(UploadMethod method, String ticketId) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Getting upload details for method: {}", method.getMethod());

            String url;
            if (ticketId != null && !ticketId.isEmpty()) {
                url = config.getReuploadDetailUrl(method.getMethod(), ticketId);
                logger.debug("Using reupload URL: {}", url);
            } else {
                url = config.getUploadDetailUrl(method.getMethod());
                logger.debug("Using upload URL: {}", url);
            }

            try {
                RestTemplate restTemplate = createRestTemplate();
                HttpHeaders headers = createHeaders();
                HttpEntity<String> entity = new HttpEntity<>(headers);

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
     * Complete submission by sending summary file
     */
    public CompletableFuture<String> completeSubmission(Submission submission, String jwtToken) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Completing submission...");

            String url = config.getSubmissionCompleteUrl();

            try {
                RestTemplate restTemplate = createRestTemplate();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(Collections.singletonList(MediaType.ALL));
                headers.set("Authorization", "Bearer " + jwtToken);
                headers.add("version", config.getToolVersion());

                String payload = objectMapper.writeValueAsString(submission);
                HttpEntity<String> entity = new HttpEntity<>(payload, headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

                String result = response.getBody();
                logger.info("Submission completed successfully");
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
     * Get submission details for resubmission
     */
    public CompletableFuture<String> getSubmissionDetails() {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Getting submission details for user: {}", username);

            String url = config.getSubmissionDetailUrl();

            try {
                RestTemplate restTemplate = createRestTemplate();
                HttpHeaders headers = createHeaders();
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

                String result = response.getBody();
                logger.info("Submission details retrieved");
                return result;

            } catch (Exception e) {
                logger.error("Failed to get submission details: {}", e.getMessage(), e);
                throw new ApiException("Failed to get submission details: " + e.getMessage(), e);
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

        if (config.isProxyEnabled()) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setProxy(new Proxy(
                Proxy.Type.HTTP,
                new InetSocketAddress(config.getProxyHost(), config.getProxyPort())
            ));
            factory.setConnectTimeout(30000);
            factory.setReadTimeout(60000);
            restTemplate.setRequestFactory(factory);
        }

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
