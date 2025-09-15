package uk.ac.ebi.pride.gui.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;

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

    // JSON parser for GitHub API responses
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String updateUrl;
    private static String fallbackDownloadUrl;

    private static String toolCurrentVersion;
    private static String latestVersion;
    private static String releaseNotes;
    private static String releaseUrl;
    private static String downloadAssetUrl;

    public UpdateChecker(String updateUrl, String downloadUrl) {
        this.updateUrl = updateUrl;
        fallbackDownloadUrl = downloadUrl;
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
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            
            int response = connection.getResponseCode();
            logger.debug("GitHub API response code: {}", response);
            
            // Handle rate limiting
            if (response == 403 || response == 429) {
                String rateLimitRemaining = connection.getHeaderField("X-RateLimit-Remaining");
                String rateLimitReset = connection.getHeaderField("X-RateLimit-Reset");
                logger.warn("GitHub API rate limit exceeded. Remaining: {}, Reset: {}", 
                    rateLimitRemaining, rateLimitReset);
                return false;
            }
            
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
                
                // Parse JSON response
                JsonNode rootNode = objectMapper.readTree(jsonResponse);
                
                // Handle both single release and array of releases
                if (rootNode.isArray()) {
                    // Multiple releases - find the latest stable release
                    toUpdate = processReleasesArray(rootNode, currentVersion);
                } else {
                    // Single release
                    toUpdate = processSingleRelease(rootNode, currentVersion);
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
     * Process a single release from GitHub API response
     */
    private boolean processSingleRelease(JsonNode releaseNode, String currentVersion) {
        try {
            // Skip prereleases
            if (releaseNode.has("prerelease") && releaseNode.get("prerelease").asBoolean()) {
                logger.debug("Skipping prerelease: {}", releaseNode.get("tag_name").asText());
                return false;
            }
            
            String tagName = releaseNode.get("tag_name").asText();
            String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;
            
            logger.info("Latest GitHub release version: {}", version);
            
            if (isHigherVersion(currentVersion, version)) {
                latestVersion = version;
                extractReleaseInfo(releaseNode);
                logger.info("Update available: {} -> {}", currentVersion, version);
                return true;
            } else {
                logger.info("No update available. Current: {}, Latest: {}", currentVersion, version);
                return false;
            }
        } catch (Exception e) {
            logger.warn("Error processing single release", e);
            return false;
        }
    }
    
    /**
     * Process an array of releases from GitHub API response
     */
    private boolean processReleasesArray(JsonNode releasesArray, String currentVersion) {
        try {
            Iterator<JsonNode> releases = releasesArray.elements();
            
            while (releases.hasNext()) {
                JsonNode release = releases.next();
                
                // Skip prereleases
                if (release.has("prerelease") && release.get("prerelease").asBoolean()) {
                    logger.debug("Skipping prerelease: {}", release.get("tag_name").asText());
                    continue;
                }
                
                String tagName = release.get("tag_name").asText();
                String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                
                logger.debug("Checking release version: {}", version);
                
                if (isHigherVersion(currentVersion, version)) {
                    latestVersion = version;
                    extractReleaseInfo(release);
                    logger.info("Update available: {} -> {}", currentVersion, version);
                    return true;
                }
            }
            
            logger.info("No update available. Current: {}", currentVersion);
            return false;
        } catch (Exception e) {
            logger.warn("Error processing releases array", e);
            return false;
        }
    }
    
    /**
     * Extract additional release information from JSON node
     */
    private void extractReleaseInfo(JsonNode releaseNode) {
        try {
            // Extract release notes
            if (releaseNode.has("body") && !releaseNode.get("body").isNull()) {
                releaseNotes = releaseNode.get("body").asText();
                // Properly unescape JSON strings
                releaseNotes = releaseNotes.replace("\\n", "\n")
                                         .replace("\\\"", "\"")
                                         .replace("\\t", "\t")
                                         .replace("\\\\", "\\");
            }
            
            // Extract release URL
            if (releaseNode.has("html_url") && !releaseNode.get("html_url").isNull()) {
                releaseUrl = releaseNode.get("html_url").asText();
            }
            
            // Extract download asset URL (prefer zip files)
            if (releaseNode.has("assets") && releaseNode.get("assets").isArray()) {
                JsonNode assets = releaseNode.get("assets");
                Iterator<JsonNode> assetIterator = assets.elements();
                
                while (assetIterator.hasNext()) {
                    JsonNode asset = assetIterator.next();
                    if (asset.has("browser_download_url") && asset.has("name")) {
                        String assetName = asset.get("name").asText();
                        String assetUrl = asset.get("browser_download_url").asText();
                        
                        // Prefer zip files, but accept any asset if no zip is found
                        if (assetName.endsWith(".zip")) {
                            downloadAssetUrl = assetUrl;
                            logger.debug("Found zip asset: {}", assetName);
                            break;
                        } else if (downloadAssetUrl == null) {
                            downloadAssetUrl = assetUrl;
                            logger.debug("Found fallback asset: {}", assetName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error extracting release info", e);
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
        showUpdateDialog(false);
    }

    /**
     * Show update dialog with option to force update
     * @param forcedUpdate if true, shows a non-dismissible dialog that forces update
     */
    public static void showUpdateDialog(boolean forcedUpdate) {
        showUpdateDialog(forcedUpdate, null, null);
    }

    /**
     * Show update dialog with option to force update and version info
     * @param forcedUpdate if true, shows a non-dismissible dialog that forces update
     * @param currentVersion current application version
     * @param latestVersion latest available version
     */
    public static void showUpdateDialog(boolean forcedUpdate, String currentVersion, String latestVersion) {
        StringBuilder message = new StringBuilder();
        message.append("<html><b>A new version of PX Submission Tool is available!</b><br><br>");
        
        // Use provided versions or fallback to static variables
        String displayCurrentVersion = currentVersion != null ? currentVersion : toolCurrentVersion;
        String displayLatestVersion = latestVersion != null ? latestVersion : UpdateChecker.latestVersion;
        
        message.append("<b>Current Version:</b> ").append(displayCurrentVersion != null ? displayCurrentVersion : "Unknown").append("<br>");
        message.append("<b>Latest Version:</b> ").append(displayLatestVersion != null ? displayLatestVersion : "Unknown").append("<br><br>");
        
        if (releaseNotes != null && !releaseNotes.trim().isEmpty()) {
            message.append("<b>What's New:</b><br>");
            // Limit release notes length for dialog
            String truncatedNotes = releaseNotes.length() > 500 ? 
                releaseNotes.substring(0, 500) + "..." : releaseNotes;
            message.append(truncatedNotes.replace("\n", "<br>")).append("<br><br>");
        }
        
        if (forcedUpdate) {
            message.append("<b style='color: red;'>This update is required to continue using the application.</b><br><br>");
            message.append("Please download and install the latest version to continue.</html>");
        } else {
            message.append("Would you like to download the latest version?</html>");
        }
        
        int option;
        if (forcedUpdate) {
            // For forced updates, only show OK button
            JOptionPane.showMessageDialog(
                null, 
                message.toString(), 
                "Update Required - PX Submission Tool", 
                JOptionPane.WARNING_MESSAGE
            );
            option = JOptionPane.YES_OPTION; // Always proceed with download
        }
        else {
            // For optional updates, show Yes/No dialog
            option = JOptionPane.showConfirmDialog(
                null, 
                message.toString(), 
                "Update Available - PX Submission Tool", 
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
            );
        }

        if (option == JOptionPane.YES_OPTION) {
            // Prefer direct download URL if available, otherwise use fallback URLs
            String urlToOpen;
            if (downloadAssetUrl != null) {
                urlToOpen = downloadAssetUrl;
                logger.info("Opening direct download URL: {}", urlToOpen);
            } else if (releaseUrl != null) {
                urlToOpen = releaseUrl;
                logger.info("Opening release page URL: {}", urlToOpen);
            } else {
                // Use the fallback download URL from constructor or default
                urlToOpen = fallbackDownloadUrl != null ? fallbackDownloadUrl : 
                    "https://github.com/PRIDE-Archive/px-submission-tool/releases/latest";
                logger.info("Opening fallback URL: {}", urlToOpen);
            }
            
            HttpUtil.openURL(urlToOpen);
            
            // For forced updates, exit the application after opening download
            if (forcedUpdate) {
                JOptionPane.showMessageDialog(
                    null,
                    "<html><b>Download!</b><br><br>" +
                    "Please install the new version and restart the application.<br>" +
                    "The application will now exit.</html>",
                    "Update Download",
                    JOptionPane.INFORMATION_MESSAGE
                );
                System.exit(0);
            }
            
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
     * Static version of isHigherVersion for use in static methods
     */
    private static boolean isHigherVersionStatic(String currentVersion, String newVersion) {
        try {
            String[] currentParts = currentVersion.split("\\.");
            String[] newParts = newVersion.split("\\.");
            
            // Ensure we have exactly 3 parts (major.minor.patch)
            if (currentParts.length < 3 || newParts.length < 3) {
                logger.warn("Invalid version format - expected major.minor.patch: current={}, new={}", currentVersion, newVersion);
                return false;
            }
            
            // Compare major version
            int currentMajor = Integer.parseInt(currentParts[0]);
            int newMajor = Integer.parseInt(newParts[0]);
            if (newMajor > currentMajor) return true;
            if (newMajor < currentMajor) return false;
            
            // Compare minor version
            int currentMinor = Integer.parseInt(currentParts[1]);
            int newMinor = Integer.parseInt(newParts[1]);
            if (newMinor > currentMinor) return true;
            if (newMinor < currentMinor) return false;
            
            // Compare patch version
            int currentPatch = Integer.parseInt(currentParts[2]);
            int newPatch = Integer.parseInt(newParts[2]);
            if (newPatch > currentPatch) return true;
            if (newPatch < currentPatch) return false;
            
            return false; // Versions are equal
        } catch (Exception e) {
            logger.warn("Error comparing versions: {} vs {}", currentVersion, newVersion, e);
            return false;
        }
    }

    /**
     * Check if a forced update is required
     * @param currentVersion current application version
     * @return true if forced update is required
     */
    public static boolean isForcedUpdateRequired(String currentVersion) {
        try {
            // Get the latest version from GitHub
            String latestVersion = getLatestVersionFromGitHub();
            if (latestVersion == null) {
                logger.warn("Could not fetch latest version from GitHub, skipping forced update check");
                return false;
            }
            
            logger.info("Checking forced update: current={}, latest={}", currentVersion, latestVersion);
            
            // Any new version requires forced update
            return isHigherVersionStatic(currentVersion, latestVersion);
        } catch (Exception e) {
            logger.warn("Error checking forced update requirement: {}", e.getMessage());
            return false;
        }
    }


    /**
     * Fetch the latest version from GitHub Releases API
     * @return latest version string or null if failed
     */
    public static String getLatestVersionFromGitHub() {
        try {
            String updateUrl = "https://api.github.com/repos/PRIDE-Archive/px-submission-tool/releases/latest";
            URL url = new URL(updateUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    JsonNode jsonNode = objectMapper.readTree(response.toString());
                    String latestVersion = jsonNode.get("tag_name").asText();
                    
                    // Remove 'v' prefix if present
                    if (latestVersion.startsWith("v")) {
                        latestVersion = latestVersion.substring(1);
                    }
                    
                    logger.info("Fetched latest version from GitHub: {}", latestVersion);
                    return latestVersion;
                }
            } else {
                logger.warn("Failed to fetch latest version from GitHub. Response code: {}", responseCode);
            }
        } catch (Exception e) {
            logger.warn("Error fetching latest version from GitHub: {}", e.getMessage());
        }
        return null;
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
    
    /**
     * Get the download URL for the latest version
     * @return download URL if available, null otherwise
     */
    public static String getDownloadUrl() {
        return downloadAssetUrl;
    }
    
    /**
     * Get the latest version string
     * @return latest version if available, null otherwise
     */
    public static String getLatestVersion() {
        return latestVersion;
    }
    
    /**
     * Get the release notes for the latest version
     * @return release notes if available, null otherwise
     */
    public static String getReleaseNotes() {
        return releaseNotes;
    }
}
