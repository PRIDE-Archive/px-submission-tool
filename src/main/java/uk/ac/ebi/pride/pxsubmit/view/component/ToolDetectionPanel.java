package uk.ac.ebi.pride.pxsubmit.view.component;

import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.pxsubmit.util.FileTypeDetector;
import uk.ac.ebi.pride.pxsubmit.util.FileTypeDetector.AnalysisTool;
import uk.ac.ebi.pride.pxsubmit.util.FileTypeDetector.ToolDetectionResult;

import java.util.Collection;
import java.util.List;

/**
 * Panel showing detected analysis tool with expected files checklist.
 * Displays tool name, confidence score, and lists required/optional files.
 *
 * Usage:
 * <pre>
 * ToolDetectionPanel panel = new ToolDetectionPanel();
 * panel.detectTool(model.getFiles());
 * // or bind to files
 * panel.setFiles(model.getFiles());
 * </pre>
 */
public class ToolDetectionPanel extends VBox {

    // State
    private final ObjectProperty<ToolDetectionResult> detectionResult = new SimpleObjectProperty<>();
    private final BooleanProperty expanded = new SimpleBooleanProperty(true);

    // UI Components
    private final HBox headerBox;
    private final Label toolNameLabel;
    private final Label confidenceLabel;
    private final ProgressBar confidenceBar;
    private final VBox contentBox;
    private final VBox requiredFilesBox;
    private final VBox optionalFilesBox;
    private final Label statusLabel;
    private final Button expandButton;

    // Styles
    private static final String PANEL_STYLE_DEFAULT =
            "-fx-background-color: #f8f9fa; " +
            "-fx-border-color: #dee2e6; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8;";

    private static final String PANEL_STYLE_DETECTED =
            "-fx-background-color: #e7f3ff; " +
            "-fx-border-color: #0066cc; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8;";

    private static final String PANEL_STYLE_WARNING =
            "-fx-background-color: #fff3cd; " +
            "-fx-border-color: #ffc107; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8;";

    public ToolDetectionPanel() {
        setSpacing(10);
        setPadding(new Insets(12));
        setStyle(PANEL_STYLE_DEFAULT);

        // Header
        headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("[T]");
        iconLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0066cc; " +
                          "-fx-background-color: #e7f3ff; -fx-padding: 2 6; -fx-background-radius: 4;");

        Label titleLabel = new Label("Analysis Tool Detection");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        toolNameLabel = new Label("Analyzing...");
        toolNameLabel.setStyle("-fx-text-fill: #666;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        confidenceBar = new ProgressBar(0);
        confidenceBar.setPrefWidth(80);
        confidenceBar.setMaxHeight(8);

        confidenceLabel = new Label("0%");
        confidenceLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        confidenceLabel.setMinWidth(35);

        expandButton = new Button("\u25BC"); // Down arrow
        expandButton.setStyle("-fx-background-color: transparent; -fx-padding: 2 6;");
        expandButton.setOnAction(e -> toggleExpanded());

        headerBox.getChildren().addAll(
                iconLabel, titleLabel, toolNameLabel,
                spacer, confidenceBar, confidenceLabel, expandButton
        );

        // Content (collapsible)
        contentBox = new VBox(10);
        contentBox.setPadding(new Insets(10, 0, 0, 0));

        statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: #666;");

        // Required files section
        Label requiredTitle = new Label("Required Files");
        requiredTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        requiredFilesBox = new VBox(4);
        requiredFilesBox.setPadding(new Insets(0, 0, 0, 15));

        // Optional files section
        Label optionalTitle = new Label("Optional Files");
        optionalTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        optionalFilesBox = new VBox(4);
        optionalFilesBox.setPadding(new Insets(0, 0, 0, 15));

        contentBox.getChildren().addAll(
                statusLabel,
                requiredTitle, requiredFilesBox,
                optionalTitle, optionalFilesBox
        );

        // Bind content visibility to expanded state
        contentBox.visibleProperty().bind(expanded);
        contentBox.managedProperty().bind(expanded);

        getChildren().addAll(headerBox, contentBox);

        // Listen to detection result changes
        detectionResult.addListener((obs, oldVal, newVal) -> updateDisplay());
    }

    /**
     * Detect tool from a collection of files
     */
    public void detectTool(Collection<DataFile> files) {
        ToolDetectionResult result = FileTypeDetector.detectTool(files);
        detectionResult.set(result);
    }

    /**
     * Set files and auto-detect tool
     */
    public void setFiles(Collection<DataFile> files) {
        detectTool(files);
    }

    /**
     * Update the display based on detection result
     */
    private void updateDisplay() {
        ToolDetectionResult result = detectionResult.get();

        if (result == null || result.tool() == AnalysisTool.UNKNOWN) {
            showNoToolDetected();
            return;
        }

        AnalysisTool tool = result.tool();
        double confidence = result.confidence();

        // Update header
        toolNameLabel.setText(tool.getDisplayName());
        toolNameLabel.setStyle("-fx-text-fill: #0066cc; -fx-font-weight: bold;");

        confidenceBar.setProgress(confidence);
        confidenceLabel.setText(String.format("%.0f%%", confidence * 100));

        // Color code confidence
        if (confidence >= 0.8) {
            confidenceBar.setStyle("-fx-accent: #28a745;");
            setStyle(PANEL_STYLE_DETECTED);
        } else if (confidence >= 0.5) {
            confidenceBar.setStyle("-fx-accent: #ffc107;");
            setStyle(PANEL_STYLE_WARNING);
        } else {
            confidenceBar.setStyle("-fx-accent: #dc3545;");
            setStyle(PANEL_STYLE_WARNING);
        }

        // Update status
        if (result.missingRequiredFiles().isEmpty()) {
            statusLabel.setText("All expected files found for " + tool.getDisplayName());
            statusLabel.setStyle("-fx-text-fill: #28a745;");
        } else {
            statusLabel.setText("Some expected files are missing. Check the list below.");
            statusLabel.setStyle("-fx-text-fill: #dc3545;");
        }

        // Update required files
        requiredFilesBox.getChildren().clear();
        for (String pattern : tool.getRequiredPatterns()) {
            boolean found = result.foundRequiredFiles().contains(pattern);
            requiredFilesBox.getChildren().add(createFileItem(pattern, found, true));
        }

        if (tool.getRequiredPatterns().isEmpty()) {
            Label noReq = new Label("No specific files required");
            noReq.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            requiredFilesBox.getChildren().add(noReq);
        }

        // Update optional files
        optionalFilesBox.getChildren().clear();
        for (String pattern : tool.getOptionalPatterns()) {
            boolean found = result.foundOptionalFiles().contains(pattern);
            optionalFilesBox.getChildren().add(createFileItem(pattern, found, false));
        }

        if (tool.getOptionalPatterns().isEmpty()) {
            Label noOpt = new Label("No optional files defined");
            noOpt.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            optionalFilesBox.getChildren().add(noOpt);
        }
    }

    private void showNoToolDetected() {
        toolNameLabel.setText("No tool detected");
        toolNameLabel.setStyle("-fx-text-fill: #666;");

        confidenceBar.setProgress(0);
        confidenceLabel.setText("0%");

        setStyle(PANEL_STYLE_DEFAULT);

        statusLabel.setText(
                "Could not detect a specific analysis tool. " +
                "Add analysis output files (e.g., MaxQuant evidence.txt, DIA-NN report.tsv) " +
                "to enable tool detection.");
        statusLabel.setStyle("-fx-text-fill: #666;");

        requiredFilesBox.getChildren().clear();
        optionalFilesBox.getChildren().clear();

        Label hint = new Label("Supported tools: MaxQuant, DIA-NN, FragPipe, Mascot, Proteome Discoverer, Skyline");
        hint.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        requiredFilesBox.getChildren().add(hint);
    }

    private HBox createFileItem(String pattern, boolean found, boolean required) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.CENTER_LEFT);

        // Status indicator
        Circle indicator = new Circle(5);
        if (found) {
            indicator.setFill(Color.web("#28a745"));
        } else if (required) {
            indicator.setFill(Color.web("#dc3545"));
        } else {
            indicator.setFill(Color.web("#999"));
        }

        // File pattern
        Label patternLabel = new Label(pattern);
        if (found) {
            patternLabel.setStyle("-fx-text-fill: #28a745;");
        } else if (required) {
            patternLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
        } else {
            patternLabel.setStyle("-fx-text-fill: #999;");
        }

        // Status text
        Label statusText = new Label();
        if (found) {
            statusText.setText("\u2714 Found");
            statusText.setStyle("-fx-text-fill: #28a745; -fx-font-size: 10px;");
        } else if (required) {
            statusText.setText("\u2718 Missing");
            statusText.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 10px;");
        } else {
            statusText.setText("Not found");
            statusText.setStyle("-fx-text-fill: #999; -fx-font-size: 10px;");
        }

        item.getChildren().addAll(indicator, patternLabel, statusText);
        return item;
    }

    private void toggleExpanded() {
        expanded.set(!expanded.get());
        expandButton.setText(expanded.get() ? "\u25BC" : "\u25B6"); // Down or Right arrow
    }

    // ==================== Public API ====================

    /**
     * Get the detection result
     */
    public ToolDetectionResult getDetectionResult() {
        return detectionResult.get();
    }

    public ObjectProperty<ToolDetectionResult> detectionResultProperty() {
        return detectionResult;
    }

    /**
     * Check if a tool was confidently detected
     */
    public boolean isToolDetected() {
        ToolDetectionResult result = detectionResult.get();
        return result != null && result.isConfident();
    }

    /**
     * Get the detected tool
     */
    public AnalysisTool getDetectedTool() {
        ToolDetectionResult result = detectionResult.get();
        return result != null ? result.tool() : AnalysisTool.UNKNOWN;
    }

    /**
     * Check if all required files are present
     */
    public boolean hasAllRequiredFiles() {
        ToolDetectionResult result = detectionResult.get();
        return result != null && result.missingRequiredFiles().isEmpty();
    }

    /**
     * Get list of missing required files
     */
    public List<String> getMissingRequiredFiles() {
        ToolDetectionResult result = detectionResult.get();
        return result != null ? result.missingRequiredFiles() : List.of();
    }

    /**
     * Set expanded state
     */
    public void setExpanded(boolean expand) {
        expanded.set(expand);
        expandButton.setText(expand ? "\u25BC" : "\u25B6");
    }

    public boolean isExpanded() {
        return expanded.get();
    }

    public BooleanProperty expandedProperty() {
        return expanded;
    }

    /**
     * Refresh detection
     */
    public void refresh(Collection<DataFile> files) {
        detectTool(files);
    }
}
