package uk.ac.ebi.pride.pxsubmit.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application configuration.
 * Loads settings from properties files and provides typed access.
 */
public class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    // Singleton instance
    private static AppConfig instance;

    // Properties
    private final Properties properties = new Properties();

    // API URLs
    private String userLoginUrl;
    private String uploadDetailUrl;
    private String reuploadDetailUrl;
    private String uploadVerifyUrl;
    private String submissionCompleteUrl;
    private String resubmissionCompleteUrl;
    private String submissionDetailUrl;
    private String submissionWsBaseUrl;

    // Tool info
    private String toolName;
    private String toolVersion;

    // Proxy settings
    private String proxyHost;
    private int proxyPort;
    private boolean proxyEnabled;

    private AppConfig() {
        loadProperties();
        parseProperties();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    /**
     * Load properties from resource files
     */
    private void loadProperties() {
        // Load setting.prop
        loadPropertyFile("/prop/setting.prop");

        // Load config.properties if exists (user overrides)
        loadPropertyFile("/config/config.properties");
    }

    private void loadPropertyFile(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                properties.load(is);
                logger.debug("Loaded properties from: {}", path);
            }
        } catch (IOException e) {
            logger.warn("Could not load properties file: {}", path, e);
        }
    }

    /**
     * Parse loaded properties into typed fields
     */
    private void parseProperties() {
        // API URLs
        userLoginUrl = getProperty("px.user.login.url",
            "https://www.proteomexchange.org/archive-submission-ws/user/login");
        uploadDetailUrl = getProperty("px.upload.detail.url",
            "https://www.proteomexchange.org/archive-submission-ws/submission/upload/{method}");
        reuploadDetailUrl = getProperty("px.reupload.detail.url",
            "https://www.proteomexchange.org/archive-submission-ws/resubmission/upload/{method}/{ticketId}");
        uploadVerifyUrl = getProperty("px.upload.verify.url",
            "https://www.proteomexchange.org/archive-submission-ws/submission/fileListAndSize");
        submissionCompleteUrl = getProperty("px.submission.complete.url",
            "https://www.proteomexchange.org/archive-submission-ws/submission/submit");
        resubmissionCompleteUrl = getProperty("px.resubmission.complete.url",
            "https://www.proteomexchange.org/archive-submission-ws/resubmission/resubmit");
        submissionDetailUrl = getProperty("px.submission.detail.url",
            "https://www.proteomexchange.org/archive-submission-ws/resubmission/projects");
        submissionWsBaseUrl = getProperty("px.submission.ws.base.url",
            "https://www.proteomexchange.org/archive-submission-ws/");

        // Tool info
        toolName = getProperty("px.submission.tool.name", "PX Submission Tool");
        toolVersion = getProperty("px.submission.tool.version", "3.0.0");

        // Proxy settings
        proxyHost = getProperty("px.proxy.host", "");
        proxyPort = getIntProperty("px.proxy.port", 0);
        proxyEnabled = !proxyHost.isEmpty() && proxyPort > 0;

        logger.info("Configuration loaded - Tool: {} v{}", toolName, toolVersion);
    }

    /**
     * Get a property value with default
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Get a property value
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Get an integer property
     */
    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer property {}: {}", key, value);
            }
        }
        return defaultValue;
    }

    /**
     * Get a boolean property
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue;
    }

    // ==================== Getters ====================

    public String getUserLoginUrl() {
        return userLoginUrl;
    }

    public String getUploadDetailUrl() {
        return uploadDetailUrl;
    }

    public String getReuploadDetailUrl() {
        return reuploadDetailUrl;
    }

    public String getUploadVerifyUrl() {
        return uploadVerifyUrl;
    }

    public String getSubmissionCompleteUrl() {
        return submissionCompleteUrl;
    }

    public String getResubmissionCompleteUrl() {
        return resubmissionCompleteUrl;
    }

    public String getSubmissionDetailUrl() {
        return submissionDetailUrl;
    }

    public String getSubmissionWsBaseUrl() {
        return submissionWsBaseUrl;
    }

    public String getToolName() {
        return toolName;
    }

    public String getToolVersion() {
        return toolVersion;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public boolean isProxyEnabled() {
        return proxyEnabled;
    }

    /**
     * Get full URL by replacing path variables
     */
    public String getUploadDetailUrl(String method) {
        return uploadDetailUrl.replace("{method}", method);
    }

    public String getReuploadDetailUrl(String method, String ticketId) {
        return reuploadDetailUrl
            .replace("{method}", method)
            .replace("{ticketId}", ticketId);
    }
}
