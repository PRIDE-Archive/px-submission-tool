package uk.ac.ebi.pride.gui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.gui.data.Credentials;
import uk.ac.ebi.pride.gui.task.GetPXSubmissionDetailTask;

import java.util.Base64;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to parse file list responses from the archive-submission-ws API
 * 
 * Parses the response from the getFileListAndSize endpoint which returns
 * a string containing the result of 'ls -lart' command execution.
 */
public class FileListParser {
    
    private static final Logger logger = LoggerFactory.getLogger(FileListParser.class);
    
    // Pattern to match ls -l output lines
    // Format: permissions links owner group size month day time/year filename
    private static final Pattern LS_PATTERN = Pattern.compile(
        "^([\\-dlcbpsrwxStT]+)\\s+" +  // permissions
        "(\\d+)\\s+" +                 // links
        "(\\S+)\\s+" +                 // owner
        "(\\S+)\\s+" +                 // group
        "(\\d+)\\s+" +                 // size
        "(\\w{3})\\s+" +              // month
        "(\\d{1,2})\\s+" +            // day
        "([\\d:]{4,5}|\\d{4})\\s+" +  // time or year
        "(.+)$"                        // filename
    );
    
    // Date formatters for parsing timestamps
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd HH:mm yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH);
    
    /**
     * Represents a file entry from the server
     */
    public static class FileEntry {
        private final String name;
        private final long sizeBytes;
        private final Instant modified;
        private final String permissions;
        private final String owner;
        private final String group;
        private final boolean directory;
        
        public FileEntry(String name, long sizeBytes, Instant modified, 
                         String permissions, String owner, String group, boolean directory) {
            this.name = name;
            this.sizeBytes = sizeBytes;
            this.modified = modified;
            this.permissions = permissions;
            this.owner = owner;
            this.group = group;
            this.directory = directory;
        }
        
        public String getName() { return name; }
        public long getSizeBytes() { return sizeBytes; }
        public Instant getModified() { return modified; }
        public String getPermissions() { return permissions; }
        public String getOwner() { return owner; }
        public String getGroup() { return group; }
        public boolean isDirectory() { return directory; }
        
        @Override
        public String toString() {
            return String.format("FileEntry{name='%s', size=%d, modified=%s, perms='%s', owner='%s', group='%s', dir=%s}",
                name, sizeBytes, modified, permissions, owner, group, directory);
        }
    }
    
    /**
     * Fetch file list from the API and parse it
     * 
     * @param baseUrl the base URL of the archive-submission-ws API
     * @param filePath the file path to query (e.g., "pride-drop-002/selva_cdsncsd")
     * @param credentials user credentials for authentication
     * @return list of FileEntry objects
     * @throws Exception if the API call fails
     */
    public static List<FileEntry> fetchFileList(String baseUrl, String filePath, Credentials credentials) throws Exception {
        // Build URL with query parameter
        String separator = baseUrl.contains("?") ? "&" : "?";
        String urlString = baseUrl + separator + "filePath=" + filePath;
        
        logger.debug("Fetching file list from: {}", urlString);
        
        RestTemplate restTemplate = new RestTemplate();
        
        try {
            // Set proxy if provided (same as other tasks)
            GetPXSubmissionDetailTask.setProxyIfProvided(restTemplate);
            logger.debug("Proxy configuration applied to RestTemplate");
            
            // Create headers with Basic Auth
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            if (credentials != null) {
                String authStr = credentials.getUsername() + ":" + credentials.getPassword();
                String base64Creds = Base64.getEncoder().encodeToString(authStr.getBytes());
                headers.add("Authorization", "Basic " + base64Creds);
                logger.debug("Authentication headers prepared for user: {}", credentials.getUsername());
            }
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Make the API call with query parameter
            String responseBody = restTemplate.exchange(urlString, HttpMethod.GET, entity, String.class).getBody();
            
            logger.debug("API response body: {}", responseBody);
            
            return parseFileListResponse(responseBody);
            
        } catch (Exception e) {
            logger.error("Failed to fetch file list from API: {}", urlString, e);
            throw e;
        }
    }
    
    /**
     * Parse the response from the API into FileEntry objects
     * 
     * @param responseBody the raw response from the API
     * @return list of FileEntry objects
     */
    public static List<FileEntry> parseFileListResponse(String responseBody) {
        List<FileEntry> entries = new ArrayList<>();
        
        logger.debug("Raw response body: {}", responseBody);
        
        // Extract the STDOUT section from the response
        String stdout = extractStdout(responseBody);
        logger.debug("Extracted STDOUT: {}", stdout);
        
        // Normalize the stdout: remove "total" line and ensure proper spacing
        // Handle compressed format like "total24-rw-rw----1..." by inserting spaces
        String normalized = normalizeLsOutput(stdout);
        logger.debug("Normalized output: {}", normalized);
        
        // Parse each line of the ls output
        String[] lines = normalized.split("\\R");
        if (lines.length == 1 && !normalized.contains("\n")) {
            // Single line - try to split by detecting file entry patterns
            lines = splitCompressedLines(normalized);
        }
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Skip "total" lines
            if (line.matches("^total\\s*\\d+\\s*$") || line.startsWith("total")) {
                logger.debug("Skipping total line: {}", line);
                continue;
            }
            
            logger.debug("Attempting to parse line: {}", line);
            FileEntry entry = parseLsLine(line);
            if (entry != null) {
                entries.add(entry);
                logger.debug("Parsed file entry: {}", entry);
            } else {
                logger.warn("Failed to parse line: {}", line);
            }
        }
        
        logger.info("Parsed {} file entries from response", entries.size());
        return entries;
    }
    
    /**
     * Extract the STDOUT section from the API response
     */
    private static String extractStdout(String responseBody) {
        // Try case-insensitive search for STDOUT
        int stdoutIndex = responseBody.indexOf("STDOUT:");
        if (stdoutIndex < 0) {
            stdoutIndex = responseBody.indexOf("stdout:");
        }
        if (stdoutIndex < 0) {
            stdoutIndex = responseBody.indexOf("Stdout:");
        }
        
        if (stdoutIndex >= 0) {
            String stdout = responseBody.substring(stdoutIndex + "STDOUT:".length()).trim();
            logger.debug("Extracted STDOUT section: {}", stdout);
            return stdout;
        }
        
        // Fallback: assume the entire response is the ls output
        logger.debug("No STDOUT marker found, treating entire response as ls output");
        logger.debug("Full response body: {}", responseBody);
        return responseBody.trim();
    }
    
    /**
     * Normalize ls output by ensuring proper spacing
     * Only applies normalization if input appears to be compressed (lacks spaces)
     */
    private static String normalizeLsOutput(String stdout) {
        // Remove "total" lines (e.g., "total24" -> "")
        stdout = stdout.replaceAll("(?i)^total\\s*\\d+", "");
        
        // Check if input is already properly formatted
        // Look for properly formatted ls lines: permissions SPACE number SPACE word SPACE word SPACE number
        // Pattern should have at least 4 spaces between permissions and the first number after owner/group
        String[] lines = stdout.split("\\R");
        boolean allProperlyFormatted = true;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.matches("^total\\s*\\d*$")) continue;
            
            // Count spaces after permissions - properly formatted should have multiple spaces
            // Pattern: -rw-rw---- 1 owner group size...
            if (line.matches("^[-dlcbpsrwxStT]{10}\\s+\\d+\\s+\\S+\\s+\\S+\\s+\\d+.*")) {
                // Properly formatted line - count spaces to be sure
                String afterPerms = line.substring(10);  // Skip permissions
                long spaceCount = afterPerms.chars().filter(c -> c == ' ').count();
                // Properly formatted should have at least 4 spaces (after perms, links, owner, group)
                if (spaceCount >= 4) {
                    continue;  // This line is fine
                }
            }
            // Line doesn't match pattern or has too few spaces - needs normalization
            allProperlyFormatted = false;
            break;
        }
        
        // If all lines are already properly formatted, just return without normalization
        if (allProperlyFormatted) {
            return stdout.trim();
        }
        
        // Input appears compressed - apply normalization carefully
        // Only normalize lines that are actually compressed
        StringBuilder result = new StringBuilder();
        for (String line : stdout.split("\\R")) {
            String normalizedLine = line;
            if (!line.trim().isEmpty() && !line.matches("^total\\s*\\d*$")) {
                // Only normalize if line doesn't already have proper spacing
                if (!line.matches(".*[-dlcbpsrwxStT]{10}\\s+\\d+\\s+\\S+\\s+\\S+\\s+\\d+.*")) {
                    // Apply normalization to compressed line
                    // Ensure space after permissions
                    normalizedLine = normalizedLine.replaceAll("([-dlcbpsrwxStT]{10})(\\d+)", "$1 $2");
                    // Ensure space after link count before owner
                    normalizedLine = normalizedLine.replaceAll("(\\s*\\d+)([A-Za-z])", "$1 $2");
                    // Ensure space before size (3+ digits after word)
                    normalizedLine = normalizedLine.replaceAll("([a-zA-Z0-9_-])(\\d{3,})", "$1 $2");
                }
            }
            if (result.length() > 0) {
                result.append("\n");
            }
            result.append(normalizedLine);
        }
        
        return result.toString().trim();
    }
    
    /**
     * Split compressed single-line output into individual file entries
     * Detects file entry patterns that start with permissions like "-rw-rw----"
     */
    private static String[] splitCompressedLines(String stdout) {
        List<String> lines = new ArrayList<>();
        
        // Pattern to detect start of a file entry: permissions pattern followed by digits
        Pattern entryPattern = Pattern.compile("([-dlcbpsrwxStT]{10})\\s*(\\d+)");
        Matcher matcher = entryPattern.matcher(stdout);
        
        List<Integer> entryStarts = new ArrayList<>();
        while (matcher.find()) {
            entryStarts.add(matcher.start());
        }
        
        // If we found entry patterns, split on them
        if (!entryStarts.isEmpty()) {
            for (int i = 0; i < entryStarts.size(); i++) {
                int start = entryStarts.get(i);
                int end = (i + 1 < entryStarts.size()) ? entryStarts.get(i + 1) : stdout.length();
                String entry = stdout.substring(start, end).trim();
                if (!entry.isEmpty() && !entry.matches("^total\\s*\\d*$")) {
                    lines.add(entry);
                }
            }
        }
        
        // If no pattern matches, return the original string as single line
        if (lines.isEmpty()) {
            // Try to parse as-is
            return new String[]{stdout};
        }
        
        return lines.toArray(new String[0]);
    }
    
    /**
     * Parse a single line from ls -l output
     */
    private static FileEntry parseLsLine(String line) {
        Matcher matcher = LS_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }
        
        try {
            String permissions = matcher.group(1);
            String owner = matcher.group(3);
            String group = matcher.group(4);
            long size = Long.parseLong(matcher.group(5));
            String month = matcher.group(6);
            String day = matcher.group(7);
            String timeOrYear = matcher.group(8);
            String filename = matcher.group(9);
            
            // Determine if it's a directory
            boolean isDirectory = permissions.startsWith("d");
            
            // Parse the modification time
            Instant modified = parseModificationTime(month, day, timeOrYear);
            
            return new FileEntry(filename, size, modified, permissions, owner, group, isDirectory);
            
        } catch (Exception e) {
            logger.warn("Error parsing ls line: {}", line, e);
            return null;
        }
    }
    
    /**
     * Parse modification time from ls output
     */
    private static Instant parseModificationTime(String month, String day, String timeOrYear) {
        try {
            int currentYear = LocalDate.now().getYear();
            String dayPadded = String.format("%02d", Integer.parseInt(day));
            
            if (timeOrYear.contains(":")) {
                // Format: "Oct 27 12:35" (assume current year)
                String dateTimeStr = month + " " + dayPadded + " " + timeOrYear + " " + currentYear;
                LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, TIME_FORMATTER);
                return dateTime.atZone(ZoneId.systemDefault()).toInstant();
            } else {
                // Format: "Oct 27 2024" (year provided)
                String dateStr = month + " " + dayPadded + " " + timeOrYear;
                LocalDate date = LocalDate.parse(dateStr, YEAR_FORMATTER);
                return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
            }
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse modification time: {} {} {}", month, day, timeOrYear, e);
            return Instant.EPOCH;
        }
    }
    
    /**
     * Validate file transfer by comparing local and remote file lists
     * 
     * @param localFiles list of local file names and sizes
     * @param remoteFiles list of remote FileEntry objects
     * @return validation result with missing/mismatched files
     */
    public static ValidationResult validateTransfer(Map<String, Long> localFiles, List<FileEntry> remoteFiles) {
        ValidationResult result = new ValidationResult();
        
        // Create a map of remote files for quick lookup
        Map<String, FileEntry> remoteFileMap = new HashMap<>();
        for (FileEntry entry : remoteFiles) {
            if (!entry.isDirectory()) {
                remoteFileMap.put(entry.getName(), entry);
            }
        }
        
        // Check each local file against remote files
        for (Map.Entry<String, Long> localEntry : localFiles.entrySet()) {
            String fileName = localEntry.getKey();
            long localSize = localEntry.getValue();
            
            FileEntry remoteEntry = remoteFileMap.get(fileName);
            if (remoteEntry == null) {
                result.addMissingFile(fileName, localSize);
            } else if (remoteEntry.getSizeBytes() != localSize) {
                result.addSizeMismatch(fileName, localSize, remoteEntry.getSizeBytes());
            } else {
                result.addValidFile(fileName, localSize);
            }
        }
        
        // Check for extra files on remote (optional)
        for (String remoteFileName : remoteFileMap.keySet()) {
            if (!localFiles.containsKey(remoteFileName)) {
                FileEntry remoteEntry = remoteFileMap.get(remoteFileName);
                result.addExtraFile(remoteFileName, remoteEntry.getSizeBytes());
            }
        }
        
        return result;
    }
    
    /**
     * Result of file transfer validation
     */
    public static class ValidationResult {
        private final List<String> validFiles = new ArrayList<>();
        private final List<SizeMismatch> sizeMismatches = new ArrayList<>();
        private final List<MissingFile> missingFiles = new ArrayList<>();
        private final List<ExtraFile> extraFiles = new ArrayList<>();
        
        public void addValidFile(String fileName, long size) {
            validFiles.add(fileName);
        }
        
        public void addSizeMismatch(String fileName, long localSize, long remoteSize) {
            sizeMismatches.add(new SizeMismatch(fileName, localSize, remoteSize));
        }
        
        public void addMissingFile(String fileName, long localSize) {
            missingFiles.add(new MissingFile(fileName, localSize));
        }
        
        public void addExtraFile(String fileName, long remoteSize) {
            extraFiles.add(new ExtraFile(fileName, remoteSize));
        }
        
        public boolean isValid() {
            return missingFiles.isEmpty() && sizeMismatches.isEmpty();
        }
        
        public List<String> getValidFiles() { return validFiles; }
        public List<SizeMismatch> getSizeMismatches() { return sizeMismatches; }
        public List<MissingFile> getMissingFiles() { return missingFiles; }
        public List<ExtraFile> getExtraFiles() { return extraFiles; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ValidationResult{");
            sb.append("valid=").append(validFiles.size());
            sb.append(", sizeMismatches=").append(sizeMismatches.size());
            sb.append(", missing=").append(missingFiles.size());
            sb.append(", extra=").append(extraFiles.size());
            sb.append("}");
            return sb.toString();
        }
    }
    
    public static class SizeMismatch {
        private final String fileName;
        private final long localSize;
        private final long remoteSize;
        
        public SizeMismatch(String fileName, long localSize, long remoteSize) {
            this.fileName = fileName;
            this.localSize = localSize;
            this.remoteSize = remoteSize;
        }
        
        public String getFileName() { return fileName; }
        public long getLocalSize() { return localSize; }
        public long getRemoteSize() { return remoteSize; }
        
        @Override
        public String toString() {
            return String.format("SizeMismatch{file='%s', local=%d, remote=%d}", fileName, localSize, remoteSize);
        }
    }
    
    public static class MissingFile {
        private final String fileName;
        private final long localSize;
        
        public MissingFile(String fileName, long localSize) {
            this.fileName = fileName;
            this.localSize = localSize;
        }
        
        public String getFileName() { return fileName; }
        public long getLocalSize() { return localSize; }
        
        @Override
        public String toString() {
            return String.format("MissingFile{file='%s', size=%d}", fileName, localSize);
        }
    }
    
    public static class ExtraFile {
        private final String fileName;
        private final long remoteSize;
        
        public ExtraFile(String fileName, long remoteSize) {
            this.fileName = fileName;
            this.remoteSize = remoteSize;
        }
        
        public String getFileName() { return fileName; }
        public long getRemoteSize() { return remoteSize; }
        
        @Override
        public String toString() {
            return String.format("ExtraFile{file='%s', size=%d}", fileName, remoteSize);
        }
    }
}
