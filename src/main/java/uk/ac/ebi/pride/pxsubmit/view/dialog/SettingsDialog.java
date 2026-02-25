package uk.ac.ebi.pride.pxsubmit.view.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;

import java.util.prefs.Preferences;

/**
 * Settings dialog for user preferences.
 *
 * Settings categories:
 * - Appearance (theme, font size)
 * - Upload preferences (default method, concurrent uploads)
 * - Connection settings (timeouts, retries)
 * - Advanced options
 */
public class SettingsDialog extends Dialog<SettingsDialog.Settings> {

    private static final Logger logger = LoggerFactory.getLogger(SettingsDialog.class);

    // Preference keys
    private static final String PREF_UPLOAD_METHOD = "upload.defaultMethod";
    private static final String PREF_CONCURRENT_UPLOADS = "upload.concurrentUploads";
    private static final String PREF_CONNECTION_TIMEOUT = "connection.timeout";
    private static final String PREF_MAX_RETRIES = "connection.maxRetries";
    private static final String PREF_SHOW_ADVANCED = "ui.showAdvanced";
    private static final String PREF_AUTO_VALIDATE = "files.autoValidate";

    private final Preferences preferences;

    // UI Controls
    private ComboBox<UploadMethod> uploadMethodCombo;
    private Spinner<Integer> concurrentUploadsSpinner;
    private Spinner<Integer> connectionTimeoutSpinner;
    private Spinner<Integer> maxRetriesSpinner;
    private CheckBox autoValidateCheck;
    private CheckBox showAdvancedCheck;

    public SettingsDialog(Window owner) {
        preferences = Preferences.userNodeForPackage(SettingsDialog.class);

        setTitle("Settings");
        setHeaderText("Configure PX Submission Tool");
        initOwner(owner);
        setResizable(true);

        // Create dialog content
        DialogPane dialogPane = getDialogPane();
        dialogPane.setContent(createContent());
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL, ButtonType.APPLY);
        dialogPane.setMinWidth(500);

        // Load current settings
        loadSettings();

        // Handle apply button
        Button applyButton = (Button) dialogPane.lookupButton(ButtonType.APPLY);
        applyButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            saveSettings();
            event.consume(); // Don't close dialog
        });

        // Handle OK button
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                saveSettings();
                return new Settings(
                        uploadMethodCombo.getValue(),
                        concurrentUploadsSpinner.getValue(),
                        connectionTimeoutSpinner.getValue(),
                        maxRetriesSpinner.getValue(),
                        autoValidateCheck.isSelected(),
                        showAdvancedCheck.isSelected()
                );
            }
            return null;
        });
    }

    private VBox createContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // Upload section
        TitledPane uploadPane = createUploadSection();

        // Connection section
        TitledPane connectionPane = createConnectionSection();

        // Advanced section
        TitledPane advancedPane = createAdvancedSection();

        content.getChildren().addAll(uploadPane, connectionPane, advancedPane);

        return content;
    }

    private TitledPane createUploadSection() {
        GridPane grid = createFormGrid();

        // Default upload method
        Label methodLabel = new Label("Default Upload Method:");
        uploadMethodCombo = new ComboBox<>();
        uploadMethodCombo.getItems().addAll(UploadMethod.values());

        Label methodHint = new Label("Aspera is faster but requires installation");
        methodHint.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        // Concurrent uploads
        Label concurrentLabel = new Label("Concurrent Uploads:");
        concurrentUploadsSpinner = new Spinner<>(1, 10, 3);
        concurrentUploadsSpinner.setEditable(true);
        concurrentUploadsSpinner.setPrefWidth(80);

        Label concurrentHint = new Label("Number of files to upload simultaneously (FTP only)");
        concurrentHint.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        // Auto validate
        autoValidateCheck = new CheckBox("Automatically validate files after adding");

        grid.add(methodLabel, 0, 0);
        grid.add(uploadMethodCombo, 1, 0);
        grid.add(methodHint, 1, 1);

        grid.add(concurrentLabel, 0, 2);
        grid.add(concurrentUploadsSpinner, 1, 2);
        grid.add(concurrentHint, 1, 3);

        grid.add(autoValidateCheck, 0, 4, 2, 1);

        TitledPane pane = new TitledPane("Upload Settings", grid);
        pane.setCollapsible(false);
        return pane;
    }

    private TitledPane createConnectionSection() {
        GridPane grid = createFormGrid();

        // Connection timeout
        Label timeoutLabel = new Label("Connection Timeout (seconds):");
        connectionTimeoutSpinner = new Spinner<>(10, 300, 30);
        connectionTimeoutSpinner.setEditable(true);
        connectionTimeoutSpinner.setPrefWidth(80);

        // Max retries
        Label retriesLabel = new Label("Maximum Retries:");
        maxRetriesSpinner = new Spinner<>(0, 10, 3);
        maxRetriesSpinner.setEditable(true);
        maxRetriesSpinner.setPrefWidth(80);

        Label retriesHint = new Label("Number of retry attempts for failed uploads");
        retriesHint.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        grid.add(timeoutLabel, 0, 0);
        grid.add(connectionTimeoutSpinner, 1, 0);

        grid.add(retriesLabel, 0, 1);
        grid.add(maxRetriesSpinner, 1, 1);
        grid.add(retriesHint, 1, 2);

        TitledPane pane = new TitledPane("Connection Settings", grid);
        pane.setCollapsible(false);
        return pane;
    }

    private TitledPane createAdvancedSection() {
        GridPane grid = createFormGrid();

        // Show advanced options
        showAdvancedCheck = new CheckBox("Show advanced options in wizard");

        Label advancedHint = new Label("Enable additional configuration options for experienced users");
        advancedHint.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        // Reset button
        Button resetButton = new Button("Reset to Defaults");
        resetButton.setOnAction(e -> resetToDefaults());

        grid.add(showAdvancedCheck, 0, 0, 2, 1);
        grid.add(advancedHint, 0, 1, 2, 1);

        grid.add(new Separator(), 0, 2, 2, 1);
        GridPane.setMargin(resetButton, new Insets(10, 0, 0, 0));
        grid.add(resetButton, 0, 3);

        TitledPane pane = new TitledPane("Advanced", grid);
        pane.setCollapsible(false);
        return pane;
    }

    private GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(180);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        return grid;
    }

    private void loadSettings() {
        try {
            // Upload method
            String methodName = preferences.get(PREF_UPLOAD_METHOD, UploadMethod.FTP.name());
            try {
                uploadMethodCombo.setValue(UploadMethod.valueOf(methodName));
            } catch (IllegalArgumentException e) {
                uploadMethodCombo.setValue(UploadMethod.FTP);
            }

            // Concurrent uploads
            concurrentUploadsSpinner.getValueFactory().setValue(
                    preferences.getInt(PREF_CONCURRENT_UPLOADS, 3));

            // Connection timeout
            connectionTimeoutSpinner.getValueFactory().setValue(
                    preferences.getInt(PREF_CONNECTION_TIMEOUT, 30));

            // Max retries
            maxRetriesSpinner.getValueFactory().setValue(
                    preferences.getInt(PREF_MAX_RETRIES, 3));

            // Auto validate
            autoValidateCheck.setSelected(
                    preferences.getBoolean(PREF_AUTO_VALIDATE, true));

            // Show advanced
            showAdvancedCheck.setSelected(
                    preferences.getBoolean(PREF_SHOW_ADVANCED, false));

        } catch (Exception e) {
            logger.error("Failed to load settings", e);
        }
    }

    private void saveSettings() {
        try {
            // Upload method
            preferences.put(PREF_UPLOAD_METHOD, uploadMethodCombo.getValue().name());

            // Concurrent uploads
            preferences.putInt(PREF_CONCURRENT_UPLOADS, concurrentUploadsSpinner.getValue());

            // Connection timeout
            preferences.putInt(PREF_CONNECTION_TIMEOUT, connectionTimeoutSpinner.getValue());

            // Max retries
            preferences.putInt(PREF_MAX_RETRIES, maxRetriesSpinner.getValue());

            // Auto validate
            preferences.putBoolean(PREF_AUTO_VALIDATE, autoValidateCheck.isSelected());

            // Show advanced
            preferences.putBoolean(PREF_SHOW_ADVANCED, showAdvancedCheck.isSelected());

            preferences.flush();
            logger.info("Settings saved successfully");

        } catch (Exception e) {
            logger.error("Failed to save settings", e);
            DialogHelper.showError("Settings Error", "Failed to save settings: " + e.getMessage());
        }
    }

    private void resetToDefaults() {
        uploadMethodCombo.setValue(UploadMethod.FTP);
        concurrentUploadsSpinner.getValueFactory().setValue(3);
        connectionTimeoutSpinner.getValueFactory().setValue(30);
        maxRetriesSpinner.getValueFactory().setValue(3);
        autoValidateCheck.setSelected(true);
        showAdvancedCheck.setSelected(false);
    }

    /**
     * Settings data class
     */
    public record Settings(
            UploadMethod defaultUploadMethod,
            int concurrentUploads,
            int connectionTimeout,
            int maxRetries,
            boolean autoValidate,
            boolean showAdvanced
    ) {}

    /**
     * Static helper to show settings dialog
     */
    public static void show(Window owner) {
        SettingsDialog dialog = new SettingsDialog(owner);
        dialog.showAndWait();
    }

    /**
     * Get a preference value
     */
    public static int getIntPreference(String key, int defaultValue) {
        return Preferences.userNodeForPackage(SettingsDialog.class).getInt(key, defaultValue);
    }

    public static boolean getBooleanPreference(String key, boolean defaultValue) {
        return Preferences.userNodeForPackage(SettingsDialog.class).getBoolean(key, defaultValue);
    }

    public static String getStringPreference(String key, String defaultValue) {
        return Preferences.userNodeForPackage(SettingsDialog.class).get(key, defaultValue);
    }
}
