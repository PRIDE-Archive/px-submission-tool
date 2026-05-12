package uk.ac.ebi.pride.gui.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

public class SdrfValidatorClient {
    private static final String BASE_URL = "https://www.ebi.ac.uk/pride/services/sdrf-validator";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SdrfValidatorClient() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = Utils.getJacksonObjectMapper();
    }

    public List<TemplateInfo> getTemplates() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/templates"))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccessfulResponse(response);

        TemplatesResponse templatesResponse = objectMapper.readValue(response.body(), TemplatesResponse.class);
        if (templatesResponse.getTemplates().isEmpty()) {
            return getFallbackTemplates();
        }
        return templatesResponse.getTemplates();
    }

    public static List<TemplateInfo> getFallbackTemplates() {
        List<TemplateInfo> templates = new ArrayList<>();
        templates.add(new TemplateInfo("affinity-proteomics", "SDRF template for affinity-based proteomics experiments (Olink, SomaScan).", "1.0.0"));
        templates.add(new TemplateInfo("base", "Base SDRF template with infrastructure columns inherited by all proteomics templates.", "1.1.0"));
        templates.add(new TemplateInfo("cell-lines", "SDRF template for cell line samples with Cellosaurus-based annotation.", "1.1.0"));
        templates.add(new TemplateInfo("clinical-metadata", "SDRF template for clinical study samples with treatment, demographics, and lifestyle metadata.", "1.0.0"));
        templates.add(new TemplateInfo("crosslinking", "SDRF template for crosslinking mass spectrometry experiments.", "1.0.0"));
        templates.add(new TemplateInfo("dia-acquisition", "SDRF template for Data-independent acquisition experiments.", "1.1.0"));
        templates.add(new TemplateInfo("gc-ms-metabolomics", "SDRF template for GC-MS-based metabolomics experiments.", "1.0.0-dev"));
        templates.add(new TemplateInfo("human", "Human SDRF template with human-specific sample metadata fields.", "1.1.0"));
        templates.add(new TemplateInfo("human-gut", "SDRF template for human gut metaproteomics.", "1.0.0"));
        templates.add(new TemplateInfo("immunopeptidomics", "SDRF template for immunopeptidomics experiments.", "1.0.0"));
        templates.add(new TemplateInfo("invertebrates", "SDRF template for invertebrate samples.", "1.1.0"));
        templates.add(new TemplateInfo("lc-ms-metabolomics", "SDRF template for LC-MS-based metabolomics experiments.", "1.0.0-dev"));
        templates.add(new TemplateInfo("metaproteomics", "Base SDRF template for metaproteomics experiments.", "1.0.0"));
        templates.add(new TemplateInfo("ms-metabolomics", "Base SDRF template for mass spectrometry-based metabolomics.", "1.0.0-dev"));
        templates.add(new TemplateInfo("ms-proteomics", "Base SDRF template for mass spectrometry-based proteomics.", "1.1.0"));
        templates.add(new TemplateInfo("oncology-metadata", "SDRF template for cancer/oncology study samples.", "1.0.0"));
        templates.add(new TemplateInfo("plants", "SDRF template for plant samples.", "1.1.0"));
        templates.add(new TemplateInfo("sample-metadata", "SDRF template with shared sample metadata columns.", "1.0.0"));
        templates.add(new TemplateInfo("single-cell", "SDRF template for single-cell proteomics experiments.", "1.0.0"));
        templates.add(new TemplateInfo("soil", "SDRF template for soil metaproteomics.", "1.0.0"));
        templates.add(new TemplateInfo("vertebrates", "SDRF template for non-human vertebrate samples.", "1.1.0"));
        templates.add(new TemplateInfo("water", "SDRF template for aquatic metaproteomics.", "1.0.0"));
        return templates;
    }

    public ValidationResult validate(File sdrfFile, String templateName, boolean skipOntology, boolean useOlsCacheOnly)
            throws IOException, InterruptedException {
        List<String> templateNames = new ArrayList<>();
        templateNames.add(templateName);
        return validate(sdrfFile, templateNames, skipOntology, useOlsCacheOnly);
    }

    public ValidationResult validate(File sdrfFile, List<String> templateNames, boolean skipOntology, boolean useOlsCacheOnly)
            throws IOException, InterruptedException {
        validateReadableSdrfFile(sdrfFile);
        List<String> selectedTemplateNames = getSelectedTemplateNames(templateNames);
        String boundary = "PXSubmissionTool-" + UUID.randomUUID();
        byte[] requestBody = buildMultipartBody(sdrfFile, boundary);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/validate?" + buildQuery(selectedTemplateNames, skipOntology, useOlsCacheOnly)))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .header("Accept", "application/json")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return parseValidationResponse(response);
    }

    private ValidationResult parseValidationResponse(HttpResponse<String> response) throws IOException {
        String responseBody = response.body();
        if (responseBody == null || responseBody.isBlank()) {
            throw new IOException("SDRF validator API returned an empty response.");
        }

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            ValidationResult result = objectMapper.readValue(responseBody, ValidationResult.class);
            result.setRawResponse(responseBody);
            return result;
        }

        ValidationResult result = new ValidationResult();
        result.setValid(false);
        result.setRawResponse(responseBody);
        result.setErrors(parseErrorResponse(responseBody));
        result.setError_count(result.getErrors().isEmpty() ? 1 : result.getErrors().size());
        return result;
    }

    private List<ValidationIssue> parseErrorResponse(String responseBody) throws IOException {
        List<ValidationIssue> issues = new ArrayList<>();
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode detailNode = rootNode.get("detail");

        if (detailNode != null && detailNode.isArray()) {
            for (JsonNode detail : detailNode) {
                issues.add(objectMapper.treeToValue(detail, ValidationIssue.class));
            }
        } else if (detailNode != null) {
            ValidationIssue issue = new ValidationIssue();
            issue.setMsg(detailNode.isTextual() ? detailNode.asText() : detailNode.toString());
            issues.add(issue);
        } else {
            ValidationIssue issue = new ValidationIssue();
            issue.setMsg(responseBody);
            issues.add(issue);
        }

        return issues;
    }

    private void validateReadableSdrfFile(File sdrfFile) throws IOException {
        if (sdrfFile == null) {
            throw new IOException("No SDRF file was selected.");
        }
        if (!sdrfFile.exists()) {
            throw new IOException("SDRF file does not exist: " + sdrfFile.getAbsolutePath());
        }
        if (!sdrfFile.isFile()) {
            throw new IOException("Selected SDRF path is not a file: " + sdrfFile.getAbsolutePath());
        }
        if (!sdrfFile.canRead()) {
            throw new IOException("SDRF file cannot be read: " + sdrfFile.getAbsolutePath());
        }
        if (sdrfFile.length() == 0) {
            throw new IOException("SDRF file is empty: " + sdrfFile.getAbsolutePath());
        }
    }

    private List<String> getSelectedTemplateNames(List<String> templateNames) throws IOException {
        List<String> selectedTemplateNames = new ArrayList<>();
        if (templateNames != null) {
            for (String templateName : templateNames) {
                if (templateName != null && !templateName.isBlank()) {
                    selectedTemplateNames.add(templateName);
                }
            }
        }
        if (selectedTemplateNames.isEmpty()) {
            throw new IOException("No SDRF validation template was selected.");
        }
        return selectedTemplateNames;
    }

    private String buildQuery(List<String> templateNames, boolean skipOntology, boolean useOlsCacheOnly) {
        StringJoiner query = new StringJoiner("&");
        for (String templateName : templateNames) {
            query.add("template=" + encode(templateName));
        }
        query.add("skip_ontology=" + skipOntology);
        query.add("use_ols_cache_only=" + useOlsCacheOnly);
        return query.toString();
    }

    private byte[] buildMultipartBody(File sdrfFile, String boundary) throws IOException {
        String lineBreak = "\r\n";
        byte[] fileBytes = Files.readAllBytes(sdrfFile.toPath());
        String header = "--" + boundary + lineBreak
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + escapeFilename(sdrfFile.getName()) + "\"" + lineBreak
                + "Content-Type: text/tab-separated-values" + lineBreak
                + lineBreak;
        String footer = lineBreak + "--" + boundary + "--" + lineBreak;

        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];

        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);
        return body;
    }

    private void ensureSuccessfulResponse(HttpResponse<String> response) throws IOException {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("SDRF validator API returned HTTP " + status + ": " + response.body());
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String escapeFilename(String filename) {
        return filename.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TemplatesResponse {
        private List<TemplateInfo> templates = new ArrayList<>();
        private Map<String, String> legacy_mappings;

        public List<TemplateInfo> getTemplates() {
            return templates;
        }

        public void setTemplates(List<TemplateInfo> templates) {
            this.templates = templates == null ? new ArrayList<>() : templates;
        }

        public Map<String, String> getLegacy_mappings() {
            return legacy_mappings;
        }

        public void setLegacy_mappings(Map<String, String> legacy_mappings) {
            this.legacy_mappings = legacy_mappings;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TemplateInfo {
        private String name;
        private String description;
        private String version;

        public TemplateInfo() {
        }

        public TemplateInfo(String name, String description, String version) {
            this.name = name;
            this.description = description;
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @Override
        public String toString() {
            return description == null || description.isBlank() ? name : name + " - " + description;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ValidationResult {
        private boolean valid;
        private List<ValidationIssue> errors = new ArrayList<>();
        private List<ValidationIssue> warnings = new ArrayList<>();
        private int error_count;
        private int warning_count;
        private List<String> templates_used = new ArrayList<>();
        private String sdrf_pipelines_version;
        private String rawResponse;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public List<ValidationIssue> getErrors() {
            return errors;
        }

        public void setErrors(List<ValidationIssue> errors) {
            this.errors = errors == null ? new ArrayList<>() : errors;
        }

        public List<ValidationIssue> getWarnings() {
            return warnings;
        }

        public void setWarnings(List<ValidationIssue> warnings) {
            this.warnings = warnings == null ? new ArrayList<>() : warnings;
        }

        public int getError_count() {
            return error_count;
        }

        public void setError_count(int error_count) {
            this.error_count = error_count;
        }

        public int getWarning_count() {
            return warning_count;
        }

        public void setWarning_count(int warning_count) {
            this.warning_count = warning_count;
        }

        public List<String> getTemplates_used() {
            return templates_used;
        }

        public void setTemplates_used(List<String> templates_used) {
            this.templates_used = templates_used == null ? new ArrayList<>() : templates_used;
        }

        public String getSdrf_pipelines_version() {
            return sdrf_pipelines_version;
        }

        public void setSdrf_pipelines_version(String sdrf_pipelines_version) {
            this.sdrf_pipelines_version = sdrf_pipelines_version;
        }

        public String getRawResponse() {
            return rawResponse;
        }

        public void setRawResponse(String rawResponse) {
            this.rawResponse = rawResponse;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ValidationIssue {
        private List<Object> loc = new ArrayList<>();
        private String msg;
        private String message;
        private String type;

        public List<Object> getLoc() {
            return loc;
        }

        public void setLoc(List<Object> loc) {
            this.loc = loc == null ? new ArrayList<>() : loc;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String format() {
            String location = loc == null || loc.isEmpty()
                    ? ""
                    : loc.stream().map(String::valueOf).reduce((a, b) -> a + " > " + b).orElse("") + ": ";
            String text = firstNonBlank(msg, message, type);
            return location + (text == null ? "No message returned by API." : text);
        }

        private String firstNonBlank(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return null;
        }
    }
}
