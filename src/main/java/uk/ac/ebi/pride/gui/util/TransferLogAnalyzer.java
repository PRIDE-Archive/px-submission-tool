package uk.ac.ebi.pride.gui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for analyzing transfer logs and identifying common issues
 */
public class TransferLogAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(TransferLogAnalyzer.class);
    
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("session ID: (\\S+)");
    private static final Pattern ERROR_PATTERN = Pattern.compile("ERROR.*?(Transfer failed|Invalid session|Transfer incomplete|Failed to start transfer)");
    private static final Pattern RETRY_PATTERN = Pattern.compile("Attempting transfer \\(attempt (\\d+)/(\\d+)\\)");
    
    /**
     * Analyzes the transfer log file for a specific session
     * @param logFile The log file to analyze
     * @param sessionId The session ID to analyze
     * @return A list of issues found in the log
     */
    public static List<String> analyzeSessionLog(File logFile, String sessionId) {
        List<String> issues = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            boolean foundSession = false;
            int retryCount = 0;
            int maxRetries = 0;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains(sessionId)) {
                    foundSession = true;
                    
                    // Check for errors
                    Matcher errorMatcher = ERROR_PATTERN.matcher(line);
                    if (errorMatcher.find()) {
                        issues.add("Error found: " + errorMatcher.group(1));
                    }
                    
                    // Check for retries
                    Matcher retryMatcher = RETRY_PATTERN.matcher(line);
                    if (retryMatcher.find()) {
                        retryCount = Integer.parseInt(retryMatcher.group(1));
                        maxRetries = Integer.parseInt(retryMatcher.group(2));
                    }
                }
            }
            
            if (!foundSession) {
                issues.add("Session ID not found in logs");
            } else if (retryCount > 0) {
                issues.add("Transfer required " + retryCount + " retries out of " + maxRetries + " attempts");
            }
            
        } catch (IOException e) {
            logger.error("Error analyzing log file: {}", e.getMessage());
            issues.add("Error reading log file: " + e.getMessage());
        }
        
        return issues;
    }
    
    /**
     * Extracts session IDs from the log file
     * @param logFile The log file to analyze
     * @return A list of session IDs found in the log
     */
    public static List<String> extractSessionIds(File logFile) {
        List<String> sessionIds = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = SESSION_ID_PATTERN.matcher(line);
                if (matcher.find()) {
                    sessionIds.add(matcher.group(1));
                }
            }
        } catch (IOException e) {
            logger.error("Error extracting session IDs: {}", e.getMessage());
        }
        return sessionIds;
    }
    
    /**
     * Checks if a transfer was successful based on the log file
     * @param logFile The log file to analyze
     * @param sessionId The session ID to check
     * @return true if the transfer was successful, false otherwise
     */
    public static boolean isTransferSuccessful(File logFile, String sessionId) {
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            boolean foundSuccess = false;
            boolean foundError = false;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains(sessionId)) {
                    if (line.contains("Transfer validation successful")) {
                        foundSuccess = true;
                    } else if (line.contains("ERROR") && 
                             (line.contains("Transfer failed") || 
                              line.contains("Invalid session") || 
                              line.contains("Transfer incomplete"))) {
                        foundError = true;
                    }
                }
            }
            
            return foundSuccess && !foundError;
        } catch (IOException e) {
            logger.error("Error checking transfer success: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the last error message for a session
     * @param logFile The log file to analyze
     * @param sessionId The session ID to check
     * @return The last error message, or null if no error was found
     */
    public static String getLastErrorMessage(File logFile, String sessionId) {
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            String lastError = null;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains(sessionId) && line.contains("ERROR")) {
                    lastError = line;
                }
            }
            
            return lastError;
        } catch (IOException e) {
            logger.error("Error getting last error message: {}", e.getMessage());
            return null;
        }
    }
} 