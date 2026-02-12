package uk.ac.ebi.pride.pxsubmit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.pxsubmit.controller.*;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.util.DebugMode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main application class for the PX Submission Tool.
 *
 * This is the new JavaFX-based entry point that replaces the Swing-based App class.
 *
 * Features:
 * - Modern JavaFX UI with CSS styling
 * - Observable state management
 * - Background task execution
 * - Proper shutdown handling
 */
public class PxSubmitApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(PxSubmitApplication.class);

    // Application constants
    private static final String APP_TITLE = "PX Submission Tool";
    private static final String APP_VERSION = "3.0.0";
    private static final int MIN_WIDTH = 900;
    private static final int MIN_HEIGHT = 700;

    // Global instances
    private static PxSubmitApplication instance;
    private Stage primaryStage;
    private SubmissionModel model;
    private WizardController wizardController;

    // Background task executor
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread t = new Thread(r, "PxSubmit-Worker");
                t.setDaemon(true);
                return t;
            }
    );

    /**
     * Application entry point
     */
    public static void main(String[] args) {
        logger.info("Starting {} v{}", APP_TITLE, APP_VERSION);
        launch(args);
    }

    /**
     * Get the application instance
     */
    public static PxSubmitApplication getInstance() {
        return instance;
    }

    @Override
    public void init() throws Exception {
        instance = this;

        // Parse command line arguments
        parseArguments(getParameters().getRaw().toArray(new String[0]));

        // Initialize model
        model = new SubmissionModel();

        logger.info("Application initialized");
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;

        try {
            // Load main window FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
            Parent root = loader.load();

            // Get controller and configure
            wizardController = loader.getController();
            wizardController.setModel(model);
            configureWizard();

            // Create scene with CSS
            Scene scene = new Scene(root, MIN_WIDTH, MIN_HEIGHT);
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/main.css")).toExternalForm()
            );

            // Configure stage
            stage.setTitle(getWindowTitle());
            stage.setScene(scene);
            stage.setMinWidth(MIN_WIDTH);
            stage.setMinHeight(MIN_HEIGHT);

            // Set application icon
            loadAppIcon(stage);

            // Handle close request
            stage.setOnCloseRequest(this::handleCloseRequest);

            // Show window
            stage.show();
            logger.info("Application window displayed");

            // Start wizard
            wizardController.start();

        } catch (IOException e) {
            logger.error("Failed to load main window", e);
            showErrorAndExit("Failed to start application", e.getMessage());
        }
    }

    /**
     * Configure wizard steps and handlers
     */
    private void configureWizard() {
        // Set up event handlers
        wizardController.setOnCancel(this::handleCancelRequest);
        wizardController.setOnHelp(this::showHelp);
        wizardController.setOnFinish(this::handleFinish);

        // Register wizard steps in order:
        // 1. Welcome - Introduction and guidelines
        // 2. Login - PRIDE authentication
        // 3. Submission Type - Choose submission type
        // 4. File Selection - Add files (drag-drop)
        // 5. File Review - Review and adjust classifications
        // 6. Sample Metadata - Species, tissue, instrument, etc. (with SDRF parsing)
        // 7. Project Metadata - Title, description, keywords
        // 8. Summary - Review before upload
        // 9. Checksum Computation - Compute checksums for all files
        // 10. Submission - Upload and complete

        wizardController.addStep(new WelcomeStep(model));
        wizardController.addStep(new LoginStep(model));
        wizardController.addStep(new SubmissionTypeStep(model));
        wizardController.addStep(new FileSelectionStep(model));
        wizardController.addStep(new FileReviewStep(model));
        wizardController.addStep(new SampleMetadataStep(model));
        wizardController.addStep(new ProjectMetadataStep(model));
        wizardController.addStep(new SummaryStep(model));
        wizardController.addStep(new ChecksumComputationStep(model));
        wizardController.addStep(new SubmissionStep(model));

        logger.info("Wizard configured with {} steps", wizardController.getStepCount());
    }

    /**
     * Handle window close request
     */
    private void handleCloseRequest(WindowEvent event) {
        event.consume(); // Prevent default close
        handleCancelRequest();
    }

    /**
     * Handle cancel/exit request
     */
    private void handleCancelRequest() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Exit");
        alert.setHeaderText("Exit PX Submission Tool?");
        alert.setContentText("Any unsaved submission data will be lost.");
        alert.initOwner(primaryStage);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                shutdown();
            }
        });
    }

    /**
     * Handle submission finish
     */
    private void handleFinish() {
        logger.info("Submission workflow completed");

        // Show completion dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Submission Complete");
        alert.setHeaderText("Your submission has been completed successfully!");
        alert.setContentText("You will receive a confirmation email shortly.");
        alert.initOwner(primaryStage);
        alert.showAndWait();

        // Ask to start new submission or exit
        Alert newSubmissionAlert = new Alert(Alert.AlertType.CONFIRMATION);
        newSubmissionAlert.setTitle("New Submission");
        newSubmissionAlert.setHeaderText("Start a new submission?");
        newSubmissionAlert.initOwner(primaryStage);

        newSubmissionAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                restart();
            } else {
                shutdown();
            }
        });
    }

    /**
     * Show help
     */
    private void showHelp() {
        // TODO: Implement help system (HTML-based or WebView)
        logger.info("Help requested");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("PX Submission Tool Help");
        alert.setContentText(
                "For help and documentation, please visit:\n\n" +
                "https://www.ebi.ac.uk/pride/help/archive/submission\n\n" +
                "Or contact: pride-support@ebi.ac.uk"
        );
        alert.initOwner(primaryStage);
        alert.showAndWait();
    }

    /**
     * Restart application for new submission
     */
    public void restart() {
        logger.info("Restarting application for new submission");

        // Reset model
        model.reset();

        // Restart wizard
        wizardController.start();

        // Update window title
        primaryStage.setTitle(getWindowTitle());
    }

    /**
     * Shutdown application
     */
    public void shutdown() {
        logger.info("Shutting down application");

        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Close stage
        if (primaryStage != null) {
            primaryStage.close();
        }

        Platform.exit();
    }

    /**
     * Parse command line arguments
     */
    private void parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--debug", "-d" -> {
                    DebugMode.enable();
                    try {
                        DebugMode.getInstance().enableFileLogging();
                        logger.info("Debug log file: {}", DebugMode.getInstance().getLogFile());
                    } catch (IOException e) {
                        logger.warn("Could not enable file logging: {}", e.getMessage());
                    }
                }
                case "--training", "-t" -> {
                    model.setTrainingMode(true);
                    logger.info("Test mode enabled");
                }
                case "--file", "-f" -> {
                    if (i + 1 < args.length) {
                        // TODO: Load submission file
                        logger.info("Submission file: {}", args[++i]);
                    }
                }
                case "--help", "-h" -> {
                    printUsage();
                    Platform.exit();
                }
                default -> logger.warn("Unknown argument: {}", args[i]);
            }
        }
    }

    /**
     * Print usage information
     */
    private void printUsage() {
        System.out.println("PX Submission Tool v" + APP_VERSION);
        System.out.println("Usage: java -jar px-submission-tool.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --debug, -d       Enable debug mode with verbose logging");
        System.out.println("  --training, -t    Enable training mode (no actual upload)");
        System.out.println("  --file, -f FILE   Load submission from file");
        System.out.println("  --help, -h        Show this help message");
        System.out.println();
        System.out.println("Debug mode logs to: ~/.pxsubmit/logs/");
    }

    /**
     * Load application icon for both window and macOS dock
     */
    private void loadAppIcon(Stage stage) {
        // Use the PRIDE Archive logo for proper dock display
        String iconPath = "/images/pride_logo.png";

        try {
            // Load icon for JavaFX window
            try (InputStream is = getClass().getResourceAsStream(iconPath)) {
                if (is != null) {
                    stage.getIcons().add(new Image(is));
                }
            }

            // Set macOS dock icon using AWT Taskbar API (Java 9+)
            if (java.awt.Taskbar.isTaskbarSupported()) {
                java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
                if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                    try (InputStream is = getClass().getResourceAsStream(iconPath)) {
                        if (is != null) {
                            java.awt.Image awtImage = javax.imageio.ImageIO.read(is);
                            taskbar.setIconImage(awtImage);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to load application icon", e);
        } catch (UnsupportedOperationException e) {
            logger.debug("Taskbar icon not supported on this platform");
        } catch (SecurityException e) {
            logger.warn("Security exception setting taskbar icon", e);
        }
    }

    /**
     * Get window title (includes training mode indicator)
     */
    private String getWindowTitle() {
        String title = APP_TITLE + " v" + APP_VERSION;
        if (model != null && model.isTrainingMode()) {
            title += " [TEST MODE]";
        }
        return title;
    }

    /**
     * Show error alert and exit
     */
    private void showErrorAndExit(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
        Platform.exit();
    }

    // ==================== Accessors ====================

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public SubmissionModel getModel() {
        return model;
    }

    public WizardController getWizardController() {
        return wizardController;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public static String getAppVersion() {
        return APP_VERSION;
    }
}
