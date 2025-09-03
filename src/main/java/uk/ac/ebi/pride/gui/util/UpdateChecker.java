package uk.ac.ebi.pride.gui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.toolsuite.gui.desktop.Desktop;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Check whether there is a new update using GitHub Releases API
 * 
 * User: rwang
 * Date: 11-Nov-2010
 * Time: 17:19:36
 * Updated: 2025-09-02 for GitHub Releases integration
 */
public class UpdateChecker {

    public static final Logger logger = LoggerFactory.getLogger(UpdateChecker.class);

    // GitHub API response patterns
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern BODY_PATTERN = Pattern.compile("\"body\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern HTML_URL_PATTERN = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ASSETS_PATTERN = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"");

    private final String updateUrl;
    private final String downloadUrl;

    private static String toolCurrentVersion;
    private static String latestVersion;
    private static String releaseNotes;
    private static String releaseUrl;
    private static String downloadAssetUrl;

    public UpdateChecker(String updateUrl, String downloadUrl) {
        this.updateUrl = updateUrl;
        this.downloadUrl = downloadUrl;
    }

    /**
     * Check whether there is a new update using GitHub Releases API
     *
     * @param currentVersion current tool version
     * @return boolean return true if there is a new update
     */
    public boolean hasUpdate(String currentVersion) {
        toolCurrentVersion = currentVersion;
        boolean toUpdate = false;
        BufferedReader reader = null;
        
        try {
            logger.info("Checking for updates from: {}", updateUrl);
            
            URL url = URI.create(updateUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set User-Agent to avoid GitHub API rate limiting
            connection.setRequestProperty("User-Agent", "PX-Submission-Tool-UpdateChecker");
            
            int response = connection.getResponseCode();
            logger.debug("GitHub API response code: {}", response);
            
            if (response == HttpURLConnection.HTTP_OK) {
                // Parse the GitHub API JSON response
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder responseContent = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
                
                String jsonResponse = responseContent.toString();
                logger.debug("GitHub API response: {}", jsonResponse);
                
                // Extract version information
                Matcher tagMatcher = TAG_NAME_PATTERN.matcher(jsonResponse);
                if (tagMatcher.find()) {
                    String tagName = tagMatcher.group(1);
                    // Remove 'v' prefix if present (e.g., "v2.10.4" -> "2.10.4")
                    String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                    
                    logger.info("Latest GitHub release version: {}", version);
                    
                    if (isHigherVersion(currentVersion, version)) {
                        latestVersion = version;
                        toUpdate = true;
                        
                        // Extract additional release information
                        extractReleaseInfo(jsonResponse);
                        
                        logger.info("Update available: {} -> {}", currentVersion, version);
                    } else {
                        logger.info("No update available. Current: {}, Latest: {}", currentVersion, version);
                    }
                } else {
                    logger.warn("Could not extract version from GitHub API response");
                }
            } else {
                logger.warn("GitHub API returned non-OK response: {}", response);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to check for updates from GitHub", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.warn("Failed to close update checker reader", e);
                }
            }
        }
        
        return toUpdate;
    }

    /**
     * Extract additional release information from GitHub API response
     */
    private void extractReleaseInfo(String jsonResponse) {
        // Extract release notes
        Matcher bodyMatcher = BODY_PATTERN.matcher(jsonResponse);
        if (bodyMatcher.find()) {
            releaseNotes = bodyMatcher.group(1);
            // Unescape JSON strings
            releaseNotes = releaseNotes.replace("\\n", "\n").replace("\\\"", "\"");
        }
        
        // Extract release URL
        Matcher urlMatcher = HTML_URL_PATTERN.matcher(jsonResponse);
        if (urlMatcher.find()) {
            releaseUrl = urlMatcher.group(1);
        }
        
        // Extract download asset URL (look for zip file)
        Matcher assetsMatcher = ASSETS_PATTERN.matcher(jsonResponse);
        if (assetsMatcher.find()) {
            downloadAssetUrl = assetsMatcher.group(1);
        }
    }

    /**
     * Compare version strings to determine if new version is higher
     */
    private boolean isHigherVersion(String currentVersion, String newVersion) {
        try {
            // Clean version strings (remove any non-numeric parts)
            String cleanCurrent = currentVersion.replaceAll("[^0-9.]", "");
            String cleanNew = newVersion.replaceAll("[^0-9.]", "");
            
            String[] currentParts = cleanCurrent.split("\\.");
            String[] newParts = cleanNew.split("\\.");
            
            // Pad arrays to same length for comparison
            int maxLength = Math.max(currentParts.length, newParts.length);
            String[] paddedCurrent = new String[maxLength];
            String[] paddedNew = new String[maxLength];
            
            for (int i = 0; i < maxLength; i++) {
                paddedCurrent[i] = i < currentParts.length ? currentParts[i] : "0";
                paddedNew[i] = i < newParts.length ? newParts[i] : "0";
            }
            
            // Compare version parts
            for (int i = 0; i < maxLength; i++) {
                int currentPart = Integer.parseInt(paddedCurrent[i]);
                int newPart = Integer.parseInt(paddedNew[i]);
                
                if (newPart > currentPart) {
                    return true;
                } else if (newPart < currentPart) {
                    return false;
                }
            }
            
            return false; // Versions are equal
        } catch (Exception e) {
            logger.warn("Error comparing versions: {} vs {}", currentVersion, newVersion, e);
            return false;
        }
    }

    /**
     * Show update dialog with enhanced information
     */
    public static void showUpdateDialog() {
        StringBuilder message = new StringBuilder();
        message.append("<html><b>A new version of PX Submission Tool is available!</b><br><br>");
        message.append("<b>Current Version:</b> ").append(toolCurrentVersion).append("<br>");
        message.append("<b>Latest Version:</b> ").append(latestVersion).append("<br><br>");
        
        if (releaseNotes != null && !releaseNotes.trim().isEmpty()) {
            message.append("<b>What's New:</b><br>");
            // Limit release notes length for dialog
            String truncatedNotes = releaseNotes.length() > 500 ? 
                releaseNotes.substring(0, 500) + "..." : releaseNotes;
            message.append(truncatedNotes.replace("\n", "<br>")).append("<br><br>");
        }
        
        message.append("Would you like to download the latest version?</html>");
        
        int option = JOptionPane.showConfirmDialog(
            null, 
            message.toString(), 
            "Update Available - PX Submission Tool", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE
        );

        if (option == JOptionPane.YES_OPTION) {
            // Open the GitHub release page or download URL
            String urlToOpen = releaseUrl != null ? releaseUrl : 
                "https://github.com/PRIDE-Toolsuite/px-submission-tool/releases/latest";
            
            logger.info("Opening update URL: {}", urlToOpen);
            HttpUtil.openURL(urlToOpen);
            
            // Check if this is a major version update that requires restart
            if (isMajorVersionUpdate(toolCurrentVersion, latestVersion)) {
                int restartOption = JOptionPane.showConfirmDialog(
                    null,
                    "<html><b>Major version update detected!</b><br><br>" +
                    "It's recommended to restart the application after downloading the new version.<br>" +
                    "Would you like to exit now?</html>",
                    "Restart Recommended",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (restartOption == JOptionPane.YES_OPTION) {
                    logger.info("User chose to restart after major version update");
                    System.exit(0);
                }
            }
        }
    }

    /**
     * Check if this is a major version update (major.minor.patch)
     */
    private static boolean isMajorVersionUpdate(String currentVersion, String newVersion) {
        try {
            String[] currentParts = currentVersion.split("\\.");
            String[] newParts = newVersion.split("\\.");
            
            if (currentParts.length > 0 && newParts.length > 0) {
                int currentMajor = Integer.parseInt(currentParts[0]);
                int newMajor = Integer.parseInt(newParts[0]);
                
                // Also consider minor version updates as significant
                if (currentParts.length > 1 && newParts.length > 1) {
                    int currentMinor = Integer.parseInt(currentParts[1]);
                    int newMinor = Integer.parseInt(newParts[1]);
                    
                    return newMajor > currentMajor || (newMajor == currentMajor && newMinor > currentMinor);
                }
                
                return newMajor > currentMajor;
            }
        } catch (Exception e) {
            logger.warn("Error checking major version update", e);
        }
        
        return false;
    }
}
