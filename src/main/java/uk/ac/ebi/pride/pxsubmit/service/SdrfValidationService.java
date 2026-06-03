package uk.ac.ebi.pride.pxsubmit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.DataFile;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * PRIDE SDRF Validator API client (templates and batch file validation).
 */
public class SdrfValidationService {

    private static final Logger logger = LoggerFactory.getLogger(SdrfValidationService.class);

    private volatile CompletableFuture<List<String>> templatesLoadFuture;
    private List<String> templateNames = new ArrayList<>();

    public record BatchResult(
            boolean allPassed,
            Map<String, Boolean> resultsByPath,
            List<String> errors,
            List<String> warnings
    ) {
    }

    public List<File> findSdrfFiles(ObservableList<DataFile> files) {
        List<File> sdrfFiles = new ArrayList<>();
        for (DataFile df : files) {
            File file = df.getFile();
            if (file == null || !file.isFile()) {
                continue;
            }
            if (df.getFileType() == ProjectFileType.EXPERIMENTAL_DESIGN || SdrfParserService.isSdrfFile(file)) {
                sdrfFiles.add(file);
            }
        }
        return sdrfFiles;
    }

    public CompletableFuture<List<String>> loadTemplatesAsync() {
        if (!templateNames.isEmpty()) {
            return CompletableFuture.completedFuture(List.copyOf(templateNames));
        }
        CompletableFuture<List<String>> inFlight = templatesLoadFuture;
        if (inFlight != null) {
            return inFlight;
        }
        CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(this::fetchTemplatesFromApi);
        templatesLoadFuture = future;
        future.whenComplete((names, error) -> {
            if (error != null) {
                logger.warn("SDRF templates load failed", error);
                templatesLoadFuture = null;
            }
        });
        return future;
    }

    public List<String> getCachedTemplateNames() {
        return List.copyOf(templateNames);
    }

    public List<String> resolveSelectableTemplates(List<String> loaded) {
        List<String> names = loaded != null && !loaded.isEmpty() ? loaded : getCachedTemplateNames();
        if (names.isEmpty()) {
            return List.of("ms-proteomics");
        }
        return names;
    }

    public BatchResult validateBatch(List<File> sdrfFiles, SdrfValidationOptions options) {
        Map<String, Boolean> resultsByPath = new HashMap<>();
        List<String> allErrors = new ArrayList<>();
        List<String> allWarnings = new ArrayList<>();
        boolean allPassed = true;

        for (File file : sdrfFiles) {
            ValidationOutcome outcome = validateFileSync(file, options);
            boolean filePassed = outcome.valid && outcome.errors.isEmpty();
            resultsByPath.put(file.getAbsolutePath(), filePassed);
            if (!filePassed) {
                allPassed = false;
            }
            allErrors.addAll(outcome.errors);
            allWarnings.addAll(outcome.warnings);
        }
        return new BatchResult(allPassed, Map.copyOf(resultsByPath), allErrors, allWarnings);
    }

    public static String formatValidationIssues(List<String> errors, List<String> warnings) {
        StringBuilder text = new StringBuilder();
        if (errors != null && !errors.isEmpty()) {
            text.append("Errors:\n").append(joinLines(errors));
        }
        if (warnings != null && !warnings.isEmpty()) {
            if (!text.isEmpty()) {
                text.append("\n\n");
            }
            text.append("Warnings:\n").append(joinLines(warnings));
        }
        return text.toString();
    }

    private static String joinLines(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        return String.join("\n", messages);
    }

    private List<String> fetchTemplatesFromApi() {
        try {
            HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SdrfValidatorApi.TEMPLATES_URL))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.warn("SDRF templates API returned status {}", response.statusCode());
                return List.of();
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            JsonNode templatesNode = root.path("templates");
            List<String> names = new ArrayList<>();
            if (templatesNode.isArray()) {
                for (JsonNode t : templatesNode) {
                    String name = t.path("name").asText(null);
                    if (name != null && !name.isBlank()) {
                        names.add(name);
                    }
                }
            }
            Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
            templateNames = names;
            logger.info("Loaded {} SDRF validator templates from API", names.size());
            return List.copyOf(names);
        } catch (Exception e) {
            logger.warn("Failed to fetch SDRF validator templates", e);
            return List.of();
        }
    }

    private ValidationOutcome validateFileSync(File file, SdrfValidationOptions options) {
        try {
            String boundary = "----SdrfBoundary" + System.currentTimeMillis();
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            byte[] body = buildMultipartBody(boundary, file.getName(), fileBytes);
            String validateUri = SdrfValidatorApi.buildValidateUri(options);

            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(validateUri))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return parseValidationOutcome(file.getName(), response);
        } catch (Exception e) {
            logger.warn("SDRF validation API call failed for: {}", file.getName(), e);
            return new ValidationOutcome(
                    false,
                    List.of(file.getName() + ": Could not reach validator service - " + e.getMessage()),
                    List.of()
            );
        }
    }

    private ValidationOutcome parseValidationOutcome(String fileName, HttpResponse<String> response) {
        try {
            if (response.statusCode() != 200) {
                return new ValidationOutcome(
                        false,
                        List.of(fileName + ": Validator returned status " + response.statusCode()),
                        List.of()
                );
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            boolean valid = root.path("valid").asBoolean(false);

            List<String> errorMessages = new ArrayList<>();
            List<String> warningMessages = new ArrayList<>();

            JsonNode errors = root.path("errors");
            if (errors.isArray()) {
                for (JsonNode err : errors) {
                    errorMessages.add(fileName + ": " + formatValidationEntry(err));
                }
            }
            JsonNode warnings = root.path("warnings");
            if (warnings.isArray()) {
                for (JsonNode warn : warnings) {
                    warningMessages.add(fileName + ": " + formatValidationEntry(warn));
                }
            }

            return new ValidationOutcome(valid, errorMessages, warningMessages);
        } catch (Exception e) {
            logger.warn("Failed to parse SDRF validation response", e);
            return new ValidationOutcome(
                    false,
                    List.of(fileName + ": Could not parse validation result"),
                    List.of()
            );
        }
    }

    private static String formatValidationEntry(JsonNode node) {
        String message = node.path("message").asText("");
        if (message.isBlank()) {
            message = node.path("msg").asText("Unknown validation issue");
        }

        JsonNode rowNode = node.path("row");
        if (rowNode.isInt()) {
            int row = rowNode.asInt(-1);
            if (row >= 0) {
                return message + " (row " + row + ")";
            }
        }
        return message;
    }

    private byte[] buildMultipartBody(String boundary, String fileName, byte[] fileBytes) {
        String prefix = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                "Content-Type: text/tab-separated-values\r\n\r\n";
        String suffix = "\r\n--" + boundary + "--\r\n";

        byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] suffixBytes = suffix.getBytes(StandardCharsets.UTF_8);

        byte[] body = new byte[prefixBytes.length + fileBytes.length + suffixBytes.length];
        System.arraycopy(prefixBytes, 0, body, 0, prefixBytes.length);
        System.arraycopy(fileBytes, 0, body, prefixBytes.length, fileBytes.length);
        System.arraycopy(suffixBytes, 0, body, prefixBytes.length + fileBytes.length, suffixBytes.length);
        return body;
    }

    private record ValidationOutcome(boolean valid, List<String> errors, List<String> warnings) {
    }
}
