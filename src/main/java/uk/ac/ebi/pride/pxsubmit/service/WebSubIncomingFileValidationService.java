package uk.ac.ebi.pride.pxsubmit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.pxsubmit.config.AppConfig;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates selected local files with the web-submissions incoming-file API.
 */
public class WebSubIncomingFileValidationService {

    private static final Logger logger = LoggerFactory.getLogger(WebSubIncomingFileValidationService.class);
    private static final String ERROR = "ERROR";
    private static final String WARNING = "WARNING";
    private static final String FINISHED = "FINISHED";

    private final AppConfig config;

    public WebSubIncomingFileValidationService() {
        this.config = AppConfig.getInstance();
    }

    public String authenticate(String username, String password) throws ValidationException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ValidationException("Missing login credentials. Please log in again before validating files.");
        }

        String url = config.getWebSubmissionsLoginUrl();
        logger.info("Authenticating with web-submissions API for incoming-file validation");
        logger.debug("Web-submissions login URL: {}", url);

        try {
            RestTemplate restTemplate = createRestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("version", config.getToolVersion());

            Map<String, String> credentials = new LinkedHashMap<>();
            credentials.put("username", username);
            credentials.put("password", password);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(credentials, headers);
            ResponseEntity<WebSubLoginResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                WebSubLoginResponse.class
            );

            WebSubLoginResponse body = response.getBody();
            logger.info("Web-submissions login response: status={}, tokenReceived={}, info={}",
                response.getStatusCode(),
                body != null && body.hasToken(),
                body != null ? body.getInfo() : null);

            if (body == null || !body.hasToken()) {
                throw new ValidationException("File validation authentication failed: missing authentication token.");
            }
            return body.getToken();
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error during web-submissions authentication: {} - {}",
                e.getStatusCode(), e.getMessage());
            throw new ValidationException(readServerMessage(e), e);
        } catch (ResourceAccessException e) {
            logger.error("Network error during web-submissions authentication: {}", e.getMessage());
            throw new ValidationException(
                "Network error: Unable to connect to the file validation authentication server.", e);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during web-submissions authentication: {}", e.getMessage(), e);
            throw new ValidationException("Unexpected validation authentication error: " + e.getMessage(), e);
        }
    }

    public ValidationResult validate(List<DataFile> files, String bearerToken) throws ValidationException {
        if (files == null || files.isEmpty()) {
            return ValidationResult.failure(List.of("No files selected for validation."));
        }
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new ValidationException("Missing login token. Please log in again before validating files.");
        }

        String url = config.getWebSubmissionsValidateIncomingFilesUrl();
        logger.info("Validating {} incoming file(s) with web-submissions API", files.size());
        logger.debug("Incoming file validation URL: {}", url);

        try {
            RestTemplate restTemplate = createRestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(bearerToken);

            List<IncomingFileEntry> requestPayload = buildRequest(files);
            logger.info("Incoming file validation request payload: {}", requestPayload);

            HttpEntity<List<IncomingFileEntry>> entity = new HttpEntity<>(requestPayload, headers);
            ResponseEntity<ValidationResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                ValidationResponse.class
            );

            ValidationResponse body = response.getBody();
            if (body == null) {
                logger.warn("Incoming file validation response: status={}, body=null", response.getStatusCode());
                throw new ValidationException("File validation failed: empty server response.");
            }
            logger.info("Incoming file validation response: status={}, ticketId={}, state={}, severity={}, finished={}, processedFiles={}, unprocessedFiles={}, message={}",
                response.getStatusCode(),
                body.getTicketId(),
                body.getState(),
                body.getSeverity(),
                body.getFinished(),
                body.processedFileCount(),
                body.unprocessedFileCount(),
                body.getMessage());
            logger.debug("Incoming file validation response details: exceptionMessage={}, toolProfile={}, attemptedToFetchFiles={}, safeToDelete={}, lastUpdated={}",
                body.getExceptionMessage(),
                body.getToolProfile(),
                body.getAttemptedToFetchFiles(),
                body.getSafeToDelete(),
                body.getLastUpdated());
            return ValidationResult.from(body, files);
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error during incoming file validation: {} - {}",
                e.getStatusCode(), e.getMessage());
            throw new ValidationException(readServerMessage(e), e);
        } catch (ResourceAccessException e) {
            logger.error("Network error during incoming file validation: {}", e.getMessage());
            throw new ValidationException(
                "Network error: Unable to connect to the file validation server.", e);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during incoming file validation: {}", e.getMessage(), e);
            throw new ValidationException("Unexpected validation error: " + e.getMessage(), e);
        }
    }

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

    private static List<IncomingFileEntry> buildRequest(List<DataFile> files) {
        return files.stream()
            .map(IncomingFileEntry::from)
            .toList();
    }

    private static String readServerMessage(HttpClientErrorException e) {
        String body = e.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return "File validation API error: " + e.getStatusText();
        }
        return body.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static String fileName(DataFile dataFile) {
        if (dataFile == null) {
            return null;
        }
        File file = dataFile.getFile();
        return firstNonBlank(
            dataFile.getFileName(),
            file != null ? file.getName() : null,
            dataFile.getFilePath()
        );
    }

    private static boolean isChecksumFile(String name) {
        return equalsIgnoreCase(name, "checksum.txt");
    }

    private static class IncomingFileEntry {
        private String name;
        private String parentName;
        private String path;
        private Boolean insideCompressedFile;
        private Integer totalFilesInParentCompressed;
        private Long size;
        private Boolean checksum;
        private Boolean directory;
        private Boolean file;

        static IncomingFileEntry from(DataFile dataFile) {
            IncomingFileEntry entry = new IncomingFileEntry();
            File localFile = dataFile != null ? dataFile.getFile() : null;
            File parent = localFile != null ? localFile.getParentFile() : null;
            String name = fileName(dataFile);

            entry.setName(name);
            entry.setParentName(parent != null ? parent.getName() : null);
            entry.setPath(localFile != null ? localFile.getAbsolutePath() :
                dataFile != null ? dataFile.getFilePath() : null);
            entry.setInsideCompressedFile(false);
            entry.setSize(localFile != null ? localFile.length() : null);
            entry.setChecksum(isChecksumFile(name));
            entry.setDirectory(localFile != null && localFile.isDirectory());
            entry.setFile(localFile == null || localFile.isFile());
            return entry;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getParentName() {
            return parentName;
        }

        public void setParentName(String parentName) {
            this.parentName = parentName;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Boolean getInsideCompressedFile() {
            return insideCompressedFile;
        }

        public void setInsideCompressedFile(Boolean insideCompressedFile) {
            this.insideCompressedFile = insideCompressedFile;
        }

        public Integer getTotalFilesInParentCompressed() {
            return totalFilesInParentCompressed;
        }

        public void setTotalFilesInParentCompressed(Integer totalFilesInParentCompressed) {
            this.totalFilesInParentCompressed = totalFilesInParentCompressed;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public Boolean getChecksum() {
            return checksum;
        }

        public void setChecksum(Boolean checksum) {
            this.checksum = checksum;
        }

        public Boolean getDirectory() {
            return directory;
        }

        public void setDirectory(Boolean directory) {
            this.directory = directory;
        }

        public Boolean getFile() {
            return file;
        }

        public void setFile(Boolean file) {
            this.file = file;
        }

        @Override
        public String toString() {
            return "IncomingFileEntry{" +
                "name='" + name + '\'' +
                ", parentName='" + parentName + '\'' +
                ", path='" + path + '\'' +
                ", insideCompressedFile=" + insideCompressedFile +
                ", totalFilesInParentCompressed=" + totalFilesInParentCompressed +
                ", size=" + size +
                ", checksum=" + checksum +
                ", directory=" + directory +
                ", file=" + file +
                '}';
        }
    }

    private static class WebSubLoginResponse {
        private String token;
        private String info;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getInfo() {
            return info;
        }

        public void setInfo(String info) {
            this.info = info;
        }

        public boolean hasToken() {
            return token != null && !token.isBlank();
        }
    }

    public record ValidationResult(
        boolean valid,
        List<String> errors,
        List<String> warnings,
        String summaryMessage,
        Map<String, Boolean> fileValidByPath,
        Map<String, String> fileTypeByPath
    ) {

        public static ValidationResult from(ValidationResponse response, List<DataFile> files) {
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            Map<String, Boolean> fileValidByPath = buildFileValidityMap(files);
            Map<String, String> fileTypeByPath = new LinkedHashMap<>();

            addResponseMessage(response, errors, warnings);
            addProcessedFiles(response.getProcessedFiles(), files, fileValidByPath, fileTypeByPath, errors, warnings);
            addResponseFileTypes(response.getFiles(), files, fileTypeByPath);
            addUnprocessedFiles(response.getUnprocessedFiles(), errors);

            boolean finished = Boolean.TRUE.equals(response.getFinished())
                || FINISHED.equalsIgnoreCase(response.getState());
            boolean hasErrorSeverity = ERROR.equalsIgnoreCase(response.getSeverity());
            boolean valid = errors.isEmpty() && !hasErrorSeverity && finished;

            if (!finished) {
                errors.add("Validation did not complete.");
                valid = false;
            }

            String summary = firstNonBlank(
                response.getMessage(),
                valid ? "Incoming file validation passed." : "Incoming file validation failed."
            );

            return new ValidationResult(
                valid,
                List.copyOf(errors),
                List.copyOf(warnings),
                summary,
                Map.copyOf(fileValidByPath),
                Map.copyOf(fileTypeByPath)
            );
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(
                false,
                List.copyOf(errors),
                List.of(),
                errors.isEmpty() ? "Validation failed." : errors.get(0),
                Map.of(),
                Map.of()
            );
        }

        public String formattedDetails() {
            StringBuilder text = new StringBuilder();
            if (!isBlank(summaryMessage)) {
                text.append(summaryMessage);
            }
            if (!errors.isEmpty()) {
                if (!text.isEmpty()) {
                    text.append("\n\n");
                }
                text.append("Errors:\n");
                for (String error : errors) {
                    text.append("- ").append(error).append('\n');
                }
            }
            if (!warnings.isEmpty()) {
                if (!text.isEmpty()) {
                    text.append("\n\n");
                }
                text.append("Warnings:\n");
                for (String warning : warnings) {
                    text.append("- ").append(warning).append('\n');
                }
            }
            return text.toString().trim();
        }

        private static void addResponseMessage(
            ValidationResponse response,
            List<String> errors,
            List<String> warnings
        ) {
            if (!isBlank(response.getExceptionMessage())) {
                errors.add(response.getExceptionMessage());
            }
            if (isBlank(response.getMessage())) {
                return;
            }
            if (ERROR.equalsIgnoreCase(response.getSeverity())) {
                errors.add(response.getMessage());
            } else if (WARNING.equalsIgnoreCase(response.getSeverity())) {
                warnings.add(response.getMessage());
            }
        }

        private static void addProcessedFiles(
            List<FileTransferEntry> processedFiles,
            List<DataFile> files,
            Map<String, Boolean> fileValidByPath,
            Map<String, String> fileTypeByPath,
            List<String> errors,
            List<String> warnings
        ) {
            if (processedFiles == null) {
                return;
            }
            for (FileTransferEntry entry : processedFiles) {
                boolean fileValid = entry.isValid();
                String path = resolveAbsolutePath(files, entry);
                if (path != null) {
                    fileValidByPath.put(path, fileValid);
                    String responseFileType = entry.responseFileType();
                    if (!isBlank(responseFileType)) {
                        fileTypeByPath.put(path, responseFileType);
                    }
                }

                String severity = entry.progressSeverity();
                if (!fileValid || ERROR.equalsIgnoreCase(severity)) {
                    errors.add(describeProgress(entry));
                } else if (WARNING.equalsIgnoreCase(severity)) {
                    warnings.add(describeProgress(entry));
                }
            }
        }

        private static void addUnprocessedFiles(List<FileTransferEntry> unprocessedFiles, List<String> errors) {
            if (unprocessedFiles == null) {
                return;
            }
            for (FileTransferEntry entry : unprocessedFiles) {
                errors.add("Validation did not process file: " + entry.displayName());
            }
        }

        private static void addResponseFileTypes(
            Map<String, FileTransferEntry> responseFiles,
            List<DataFile> files,
            Map<String, String> fileTypeByPath
        ) {
            if (responseFiles == null || responseFiles.isEmpty()) {
                return;
            }
            for (FileTransferEntry entry : responseFiles.values()) {
                String path = resolveAbsolutePath(files, entry);
                String responseFileType = entry.responseFileType();
                if (path != null && !isBlank(responseFileType)) {
                    fileTypeByPath.putIfAbsent(path, responseFileType);
                }
            }
        }

        private static Map<String, Boolean> buildFileValidityMap(List<DataFile> files) {
            Map<String, Boolean> map = new LinkedHashMap<>();
            if (files == null) {
                return map;
            }
            for (DataFile file : files) {
                if (file.getFile() != null) {
                    map.put(file.getFile().getAbsolutePath(), true);
                }
            }
            return map;
        }

        private static String resolveAbsolutePath(List<DataFile> files, FileTransferEntry entry) {
            if (files == null || entry == null) {
                return null;
            }
            String responsePath = entry.getPath();
            String responseName = entry.displayName();
            for (DataFile file : files) {
                File localFile = file.getFile();
                if (localFile == null) {
                    continue;
                }
                if (responsePath != null && responsePath.equals(localFile.getAbsolutePath())) {
                    return localFile.getAbsolutePath();
                }
                if (responseName.equals(file.getFileName())
                    || responseName.equals(file.getFilePath())
                    || responseName.equals(localFile.getName())
                    || responseName.equals(localFile.getAbsolutePath())) {
                    return localFile.getAbsolutePath();
                }
            }
            return null;
        }

        private static String describeProgress(FileTransferEntry entry) {
            String message = entry.progressMessage();
            if (!isBlank(message)) {
                return entry.displayName() + ": " + message;
            }
            return "Invalid file: " + entry.displayName();
        }
    }

    public static class ValidationResponse {
        private String ticketId;
        private String message;
        private String exceptionMessage;
        private String severity;
        private String toolProfile;
        private String state;
        private Map<String, FileTransferEntry> files;
        private Long lastUpdated;
        private Boolean safeToDelete;
        private Boolean attemptedToFetchFiles;
        private Long timeStarted;
        private Long timeFinished;
        private List<FileTransferEntry> processedFiles;
        private List<FileTransferEntry> unprocessedFiles;
        private Boolean finished;

        public String getTicketId() {
            return ticketId;
        }

        public void setTicketId(String ticketId) {
            this.ticketId = ticketId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getExceptionMessage() {
            return exceptionMessage;
        }

        public void setExceptionMessage(String exceptionMessage) {
            this.exceptionMessage = exceptionMessage;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getToolProfile() {
            return toolProfile;
        }

        public void setToolProfile(String toolProfile) {
            this.toolProfile = toolProfile;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public Map<String, FileTransferEntry> getFiles() {
            return files;
        }

        public void setFiles(Map<String, FileTransferEntry> files) {
            this.files = files;
        }

        public Long getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(Long lastUpdated) {
            this.lastUpdated = lastUpdated;
        }

        public Boolean getSafeToDelete() {
            return safeToDelete;
        }

        public void setSafeToDelete(Boolean safeToDelete) {
            this.safeToDelete = safeToDelete;
        }

        public Boolean getAttemptedToFetchFiles() {
            return attemptedToFetchFiles;
        }

        public void setAttemptedToFetchFiles(Boolean attemptedToFetchFiles) {
            this.attemptedToFetchFiles = attemptedToFetchFiles;
        }

        public Long getTimeStarted() {
            return timeStarted;
        }

        public void setTimeStarted(Long timeStarted) {
            this.timeStarted = timeStarted;
        }

        public Long getTimeFinished() {
            return timeFinished;
        }

        public void setTimeFinished(Long timeFinished) {
            this.timeFinished = timeFinished;
        }

        public List<FileTransferEntry> getProcessedFiles() {
            return processedFiles;
        }

        public void setProcessedFiles(List<FileTransferEntry> processedFiles) {
            this.processedFiles = processedFiles;
        }

        public List<FileTransferEntry> getUnprocessedFiles() {
            return unprocessedFiles;
        }

        public void setUnprocessedFiles(List<FileTransferEntry> unprocessedFiles) {
            this.unprocessedFiles = unprocessedFiles;
        }

        public Boolean getFinished() {
            return finished;
        }

        public void setFinished(Boolean finished) {
            this.finished = finished;
        }

        public int processedFileCount() {
            return processedFiles != null ? processedFiles.size() : 0;
        }

        public int unprocessedFileCount() {
            return unprocessedFiles != null ? unprocessedFiles.size() : 0;
        }
    }

    public static class FileTransferEntry {
        private String id;
        private String name;
        private String ticketId;
        private String parentName;
        private Boolean insideCompressedFile;
        private Integer totalFilesInParentCompressed;
        private Long size;
        private String path;
        private String category;
        private FileValidateProgress progress;
        private Boolean checksum;
        private String medium;
        private Boolean directory;
        private Boolean file;

        public boolean isValid() {
            return progress == null || progress.getValid() == null || progress.getValid();
        }

        public String progressMessage() {
            return progress != null ? progress.getMessage() : null;
        }

        public String progressSeverity() {
            return progress != null ? progress.getSeverity() : null;
        }

        public String displayName() {
            return firstNonBlank(name, path, id, "file");
        }

        public String responseFileType() {
            return firstNonBlank(category, progress != null ? progress.getRawFileFormatId() : null);
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTicketId() {
            return ticketId;
        }

        public void setTicketId(String ticketId) {
            this.ticketId = ticketId;
        }

        public String getParentName() {
            return parentName;
        }

        public void setParentName(String parentName) {
            this.parentName = parentName;
        }

        public Boolean getInsideCompressedFile() {
            return insideCompressedFile;
        }

        public void setInsideCompressedFile(Boolean insideCompressedFile) {
            this.insideCompressedFile = insideCompressedFile;
        }

        public Integer getTotalFilesInParentCompressed() {
            return totalFilesInParentCompressed;
        }

        public void setTotalFilesInParentCompressed(Integer totalFilesInParentCompressed) {
            this.totalFilesInParentCompressed = totalFilesInParentCompressed;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public FileValidateProgress getProgress() {
            return progress;
        }

        public void setProgress(FileValidateProgress progress) {
            this.progress = progress;
        }

        public Boolean getChecksum() {
            return checksum;
        }

        public void setChecksum(Boolean checksum) {
            this.checksum = checksum;
        }

        public String getMedium() {
            return medium;
        }

        public void setMedium(String medium) {
            this.medium = medium;
        }

        public Boolean getDirectory() {
            return directory;
        }

        public void setDirectory(Boolean directory) {
            this.directory = directory;
        }

        public Boolean getFile() {
            return file;
        }

        public void setFile(Boolean file) {
            this.file = file;
        }
    }

    public static class FileValidateProgress {
        private Boolean valid;
        private String message;
        private String severity;
        private String state;
        private String rawFileFormatId;

        public Boolean getValid() {
            return valid;
        }

        public void setValid(Boolean valid) {
            this.valid = valid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getRawFileFormatId() {
            return rawFileFormatId;
        }

        public void setRawFileFormatId(String rawFileFormatId) {
            this.rawFileFormatId = rawFileFormatId;
        }
    }

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }

        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
