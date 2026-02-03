package uk.ac.ebi.pride.pxsubmit.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Debug mode utility for verbose logging and troubleshooting.
 *
 * Features:
 * - Toggle debug mode at runtime
 * - Verbose logging with timestamps
 * - Log to file option
 * - Event history for UI display
 * - System information dump
 *
 * Usage:
 *   DebugMode.enable();
 *   DebugMode.log("Something happened");
 *   DebugMode.disable();
 */
public class DebugMode {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DebugMode.class);

    // Singleton instance
    private static DebugMode instance;

    // Debug state
    private final BooleanProperty enabled = new SimpleBooleanProperty(false);

    // Event log (limited size)
    private static final int MAX_LOG_SIZE = 1000;
    private final List<LogEntry> logHistory = new CopyOnWriteArrayList<>();

    // File logging
    private PrintWriter fileWriter;
    private File logFile;

    // Listeners
    private final List<DebugLogListener> listeners = new CopyOnWriteArrayList<>();

    private DebugMode() {
        // Private constructor for singleton
    }

    public static synchronized DebugMode getInstance() {
        if (instance == null) {
            instance = new DebugMode();
        }
        return instance;
    }

    // ==================== Static convenience methods ====================

    public static void enable() {
        getInstance().setEnabled(true);
    }

    public static void disable() {
        getInstance().setEnabled(false);
    }

    public static boolean isEnabled() {
        return getInstance().enabled.get();
    }

    public static void log(String message) {
        getInstance().logMessage("INFO", message);
    }

    public static void log(String category, String message) {
        getInstance().logMessage(category, message);
    }

    public static void logError(String message, Throwable error) {
        getInstance().logMessage("ERROR", message + ": " + error.getMessage());
        if (getInstance().enabled.get()) {
            error.printStackTrace();
        }
    }

    // ==================== Instance methods ====================

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);

        if (enabled) {
            configureVerboseLogging();
            logSystemInfo();
            logMessage("DEBUG", "Debug mode ENABLED");
        } else {
            logMessage("DEBUG", "Debug mode DISABLED");
            configureNormalLogging();
        }
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    public void logMessage(String level, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        LogEntry entry = new LogEntry(timestamp, level, message);

        // Add to history
        logHistory.add(entry);
        if (logHistory.size() > MAX_LOG_SIZE) {
            logHistory.remove(0);
        }

        // Log to SLF4J
        String formattedMessage = String.format("[%s] [%s] %s", timestamp, level, message);
        if (enabled.get()) {
            logger.info(formattedMessage);
        } else {
            logger.debug(formattedMessage);
        }

        // Write to file if enabled
        if (fileWriter != null) {
            fileWriter.println(formattedMessage);
            fileWriter.flush();
        }

        // Notify listeners
        for (DebugLogListener listener : listeners) {
            listener.onLogEntry(entry);
        }
    }

    public List<LogEntry> getLogHistory() {
        return new ArrayList<>(logHistory);
    }

    public void clearHistory() {
        logHistory.clear();
    }

    // ==================== File logging ====================

    public void enableFileLogging(File file) throws IOException {
        this.logFile = file;
        this.fileWriter = new PrintWriter(new FileWriter(file, true));
        logMessage("DEBUG", "File logging enabled: " + file.getAbsolutePath());
    }

    public void enableFileLogging() throws IOException {
        String userHome = System.getProperty("user.home");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File logDir = new File(userHome, ".pxsubmit/logs");
        logDir.mkdirs();
        File file = new File(logDir, "pxsubmit_debug_" + timestamp + ".log");
        enableFileLogging(file);
    }

    public void disableFileLogging() {
        if (fileWriter != null) {
            logMessage("DEBUG", "File logging disabled");
            fileWriter.close();
            fileWriter = null;
        }
    }

    public File getLogFile() {
        return logFile;
    }

    // ==================== Listeners ====================

    public void addListener(DebugLogListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DebugLogListener listener) {
        listeners.remove(listener);
    }

    // ==================== Logging configuration ====================

    private void configureVerboseLogging() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // Set root logger to DEBUG
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.DEBUG);

        // Set pxsubmit package to TRACE for maximum detail
        Logger pxsubmitLogger = loggerContext.getLogger("uk.ac.ebi.pride.pxsubmit");
        pxsubmitLogger.setLevel(Level.TRACE);

        // Also enable detailed logging for JavaFX
        Logger javafxLogger = loggerContext.getLogger("javafx");
        javafxLogger.setLevel(Level.DEBUG);

        logger.info("Verbose logging configured");
    }

    private void configureNormalLogging() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // Reset to INFO level
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);

        Logger pxsubmitLogger = loggerContext.getLogger("uk.ac.ebi.pride.pxsubmit");
        pxsubmitLogger.setLevel(Level.INFO);

        logger.info("Normal logging configured");
    }

    // ==================== System info ====================

    private void logSystemInfo() {
        logMessage("SYSTEM", "=".repeat(50));
        logMessage("SYSTEM", "PX Submission Tool - Debug Information");
        logMessage("SYSTEM", "=".repeat(50));
        logMessage("SYSTEM", "Java Version: " + System.getProperty("java.version"));
        logMessage("SYSTEM", "Java Vendor: " + System.getProperty("java.vendor"));
        logMessage("SYSTEM", "Java Home: " + System.getProperty("java.home"));
        logMessage("SYSTEM", "OS Name: " + System.getProperty("os.name"));
        logMessage("SYSTEM", "OS Version: " + System.getProperty("os.version"));
        logMessage("SYSTEM", "OS Arch: " + System.getProperty("os.arch"));
        logMessage("SYSTEM", "User Home: " + System.getProperty("user.home"));
        logMessage("SYSTEM", "Working Dir: " + System.getProperty("user.dir"));

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        logMessage("SYSTEM", "Max Memory: " + maxMemory + " MB");
        logMessage("SYSTEM", "Total Memory: " + totalMemory + " MB");
        logMessage("SYSTEM", "Free Memory: " + freeMemory + " MB");
        logMessage("SYSTEM", "Available Processors: " + runtime.availableProcessors());
        logMessage("SYSTEM", "=".repeat(50));
    }

    public String getSystemInfoString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        sb.append("Java Vendor: ").append(System.getProperty("java.vendor")).append("\n");
        sb.append("OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append("\n");
        sb.append("Architecture: ").append(System.getProperty("os.arch")).append("\n");

        Runtime runtime = Runtime.getRuntime();
        sb.append("Max Memory: ").append(runtime.maxMemory() / (1024 * 1024)).append(" MB\n");
        sb.append("Processors: ").append(runtime.availableProcessors()).append("\n");

        return sb.toString();
    }

    // ==================== Inner classes ====================

    public record LogEntry(String timestamp, String level, String message) {
        @Override
        public String toString() {
            return String.format("[%s] [%s] %s", timestamp, level, message);
        }
    }

    public interface DebugLogListener {
        void onLogEntry(LogEntry entry);
    }
}
