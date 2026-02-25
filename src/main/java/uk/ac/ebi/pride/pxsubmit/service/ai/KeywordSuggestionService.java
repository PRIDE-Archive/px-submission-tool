package uk.ac.ebi.pride.pxsubmit.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for suggesting keywords using an AI endpoint.
 * Sends project metadata context to a configurable HTTP endpoint and parses
 * keyword suggestions from the response.
 *
 * The endpoint is expected to accept a JSON POST with {"prompt": "...", "model": "..."}
 * and return a JSON array of strings.
 */
public class KeywordSuggestionService {

    private static final Logger logger = LoggerFactory.getLogger(KeywordSuggestionService.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public KeywordSuggestionService(String apiUrl, String apiKey, String model) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Check whether the service has the minimum configuration needed to make requests.
     */
    public boolean isConfigured() {
        return apiUrl != null && !apiUrl.isBlank()
                && apiKey != null && !apiKey.isBlank();
    }

    /**
     * Suggest keywords based on project metadata fields.
     *
     * @param title            project title
     * @param description      project description
     * @param sampleProtocol   sample processing protocol
     * @param dataProtocol     data processing protocol
     * @param experimentTypes  list of experiment type names
     * @return a future list of suggested keyword strings; empty list on failure
     */
    public CompletableFuture<List<String>> suggestKeywords(String title,
                                                           String description,
                                                           String sampleProtocol,
                                                           String dataProtocol,
                                                           List<String> experimentTypes) {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        String prompt = buildPrompt(title, description, sampleProtocol, dataProtocol, experimentTypes);

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("prompt", prompt);
            if (model != null && !model.isBlank()) {
                body.put("model", model);
            }
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");

            requestBuilder.header("Authorization", "Bearer " + apiKey);

            HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            logger.warn("AI keyword suggestion failed with status {}: {}",
                                    response.statusCode(), response.body());
                            return Collections.<String>emptyList();
                        }
                        return parseResponse(response.body());
                    })
                    .exceptionally(e -> {
                        logger.error("AI keyword suggestion error: {}", e.getMessage());
                        return Collections.emptyList();
                    });
        } catch (Exception e) {
            logger.error("Failed to build AI keyword suggestion request: {}", e.getMessage());
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    private String buildPrompt(String title, String description, String sampleProtocol,
                               String dataProtocol, List<String> experimentTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Suggest up to 10 concise keywords for a proteomics dataset submission. ");
        sb.append("Return ONLY a JSON array of keyword strings, no explanation.\n\n");

        if (title != null && !title.isBlank()) {
            sb.append("Title: ").append(title).append("\n");
        }
        if (description != null && !description.isBlank()) {
            sb.append("Description: ").append(description).append("\n");
        }
        if (sampleProtocol != null && !sampleProtocol.isBlank()) {
            sb.append("Sample Protocol: ").append(sampleProtocol).append("\n");
        }
        if (dataProtocol != null && !dataProtocol.isBlank()) {
            sb.append("Data Protocol: ").append(dataProtocol).append("\n");
        }
        if (experimentTypes != null && !experimentTypes.isEmpty()) {
            sb.append("Experiment Types: ").append(String.join(", ", experimentTypes)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Parse the response body. Accepts either a plain JSON array of strings
     * or an object containing a "keywords" array.
     */
    private List<String> parseResponse(String responseBody) {
        List<String> keywords = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode arrayNode = null;
            if (root.isArray()) {
                arrayNode = root;
            } else if (root.has("keywords") && root.get("keywords").isArray()) {
                arrayNode = root.get("keywords");
            } else if (root.has("suggestions") && root.get("suggestions").isArray()) {
                arrayNode = root.get("suggestions");
            }

            if (arrayNode != null) {
                for (JsonNode node : arrayNode) {
                    String kw = node.asText().trim();
                    if (!kw.isEmpty()) {
                        keywords.add(kw);
                    }
                }
            } else {
                logger.warn("AI response did not contain a recognizable keyword array: {}",
                        responseBody.substring(0, Math.min(200, responseBody.length())));
            }
        } catch (Exception e) {
            logger.error("Error parsing AI keyword response: {}", e.getMessage());
        }
        return keywords;
    }
}
