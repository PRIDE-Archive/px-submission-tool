package uk.ac.ebi.pride.pxsubmit.view.component;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.util.Duration;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.pxsubmit.util.FileTypeDetector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Visual panel showing file classification summary.
 * Displays color-coded badges for each file type with counts and sizes.
 * Shows warning icons for missing mandatory files.
 *
 * Usage:
 * <pre>
 * FileClassificationPanel panel = new FileClassificationPanel();
 * panel.setFiles(model.getFiles());
 * // or
 * panel.filesProperty().bind(model.filesProperty());
 * </pre>
 */
public class FileClassificationPanel extends VBox {

    private final ObservableList<DataFile> files = FXCollections.observableArrayList();
    private final FlowPane badgeContainer;
    private final VBox detailsContainer;
    private final Label summaryLabel;

    // Statistics
    private final Map<ProjectFileType, List<DataFile>> filesByType = new EnumMap<>(ProjectFileType.class);
    private final IntegerProperty totalCount = new SimpleIntegerProperty(0);
    private final LongProperty totalSize = new SimpleLongProperty(0);

    // Selection callback
    private FileTypeSelectionHandler selectionHandler;

    // Display mode
    private final BooleanProperty showDetails = new SimpleBooleanProperty(true);
    private final BooleanProperty showWarnings = new SimpleBooleanProperty(true);

    // Detected tool
    private final ObjectProperty<FileTypeDetector.ToolDetectionResult> detectedTool = new SimpleObjectProperty<>();

    public FileClassificationPanel() {
        setSpacing(5);
        setPadding(new Insets(6));
        setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-background-radius: 8;");

        // Title
        Label titleLabel = new Label("File Classification");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Badge container
        badgeContainer = new FlowPane();
        badgeContainer.setHgap(10);
        badgeContainer.setVgap(8);
        badgeContainer.setAlignment(Pos.CENTER_LEFT);

        // Summary label
        summaryLabel = new Label();
        summaryLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        // Details container (optional expanded view)
        detailsContainer = new VBox(5);
        detailsContainer.managedProperty().bind(detailsContainer.visibleProperty());
        detailsContainer.visibleProperty().bind(showDetails);

        getChildren().addAll(titleLabel, badgeContainer, summaryLabel, detailsContainer);

        // Listen to file changes
        files.addListener((ListChangeListener.Change<? extends DataFile> c) -> updateDisplay());
    }

    /**
     * Update the display based on current files
     */
    private void updateDisplay() {
        // Clear and recategorize
        filesByType.clear();
        for (ProjectFileType type : ProjectFileType.values()) {
            filesByType.put(type, new ArrayList<>());
        }

        long total = 0;
        for (DataFile df : files) {
            ProjectFileType type = df.getFileType();
            if (type == null) {
                type = FileTypeDetector.detectFileType(df);
            }
            filesByType.get(type).add(df);
            if (df.getFile() != null) {
                total += df.getFile().length();
            }
        }

        totalCount.set(files.size());
        totalSize.set(total);

        // Update badges
        badgeContainer.getChildren().clear();

        // "All" badge at the start
        if (!files.isEmpty()) {
            badgeContainer.getChildren().add(createAllBadge());
        }

        for (ProjectFileType type : getDisplayOrder()) {
            List<DataFile> typeFiles = filesByType.get(type);
            if (!typeFiles.isEmpty() || (showWarnings.get() && FileTypeDetector.isMandatory(type))) {
                badgeContainer.getChildren().add(createBadge(type, typeFiles));
            }
        }

        // Update summary
        summaryLabel.setText(String.format("%d files, %s total",
                files.size(), formatSize(total)));

        // Detect tool
        FileTypeDetector.ToolDetectionResult toolResult = FileTypeDetector.detectTool(files);
        detectedTool.set(toolResult);

        // Update details
        updateDetails();
    }

    /**
     * Create a badge for a file type
     */
    private HBox createBadge(ProjectFileType type, List<DataFile> typeFiles) {
        HBox badge = new HBox(6);
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.setPadding(new Insets(6, 12, 6, 12));

        String color = FileTypeDetector.getColor(type);
        boolean isEmpty = typeFiles.isEmpty();
        boolean isMandatory = FileTypeDetector.isMandatory(type);

        // Style based on state
        String bgColor = isEmpty ? "#fff3cd" : "white";
        String borderColor = isEmpty && isMandatory ? "#ffc107" : color;

        badge.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-border-color: %s; " +
                "-fx-border-radius: 16; " +
                "-fx-background-radius: 16; " +
                "-fx-cursor: hand;",
                bgColor, borderColor));

        // Status icon
        Label statusIcon = new Label();
        if (isEmpty && isMandatory) {
            statusIcon.setText("\u26A0"); // Warning
            statusIcon.setStyle("-fx-text-fill: #ffc107;");
        } else if (isEmpty) {
            statusIcon.setText("\u2796"); // Minus
            statusIcon.setStyle("-fx-text-fill: #999;");
        } else {
            statusIcon.setText("\u2714"); // Check
            statusIcon.setStyle("-fx-text-fill: " + color + ";");
        }

        // Type label
        Label typeLabel = new Label(getShortName(type));
        typeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + ";");

        // Count
        Label countLabel = new Label(String.valueOf(typeFiles.size()));
        countLabel.setStyle("-fx-text-fill: #666;");

        // Size (if files present)
        if (!typeFiles.isEmpty()) {
            long size = typeFiles.stream()
                    .filter(f -> f.getFile() != null)
                    .mapToLong(f -> f.getFile().length())
                    .sum();
            Label sizeLabel = new Label("(" + formatSize(size) + ")");
            sizeLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
            badge.getChildren().addAll(statusIcon, typeLabel, countLabel, sizeLabel);
        } else {
            badge.getChildren().addAll(statusIcon, typeLabel, countLabel);
        }

        // Tooltip
        Tooltip tooltip = new Tooltip(createTooltipText(type, typeFiles));
        tooltip.setShowDelay(Duration.millis(300));
        Tooltip.install(badge, tooltip);

        // Click handler
        badge.setOnMouseClicked(e -> {
            if (selectionHandler != null) {
                selectionHandler.onTypeSelected(type, typeFiles);
            }
        });

        // Hover effect
        badge.setOnMouseEntered(e -> badge.setStyle(badge.getStyle() + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);"));
        badge.setOnMouseExited(e -> badge.setStyle(badge.getStyle().replace("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);", "")));

        return badge;
    }

    /**
     * Create the "All" badge that shows/filters all files
     */
    private HBox createAllBadge() {
        HBox badge = new HBox(6);
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.setPadding(new Insets(6, 12, 6, 12));
        badge.setStyle(
                "-fx-background-color: white; " +
                "-fx-border-color: #6c757d; " +
                "-fx-border-radius: 16; " +
                "-fx-background-radius: 16; " +
                "-fx-cursor: hand;");

        Label icon = new Label("\u2630"); // hamburger icon
        icon.setStyle("-fx-text-fill: #6c757d;");

        Label typeLabel = new Label("All");
        typeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #6c757d;");

        Label countLabel = new Label(String.valueOf(files.size()));
        countLabel.setStyle("-fx-text-fill: #666;");

        badge.getChildren().addAll(icon, typeLabel, countLabel);

        Tooltip tooltip = new Tooltip("Show all files");
        tooltip.setShowDelay(Duration.millis(300));
        Tooltip.install(badge, tooltip);

        badge.setOnMouseClicked(e -> {
            if (selectionHandler != null) {
                selectionHandler.onTypeSelected(null, new ArrayList<>(files));
            }
        });

        badge.setOnMouseEntered(e -> badge.setStyle(badge.getStyle() + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);"));
        badge.setOnMouseExited(e -> badge.setStyle(badge.getStyle().replace("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);", "")));

        return badge;
    }

    /**
     * Update details section
     */
    private void updateDetails() {
        detailsContainer.getChildren().clear();

        // Show detected tool if confident
        FileTypeDetector.ToolDetectionResult tool = detectedTool.get();
        if (tool != null && tool.isConfident()) {
            HBox toolBox = new HBox(8);
            toolBox.setAlignment(Pos.CENTER_LEFT);
            toolBox.setPadding(new Insets(8));
            toolBox.setStyle("-fx-background-color: #e7f3ff; -fx-border-radius: 4; -fx-background-radius: 4;");

            Label iconLabel = new Label("\uD83D\uDD27"); // Wrench emoji
            Label toolLabel = new Label("Detected: " + tool.tool().getDisplayName());
            toolLabel.setStyle("-fx-font-weight: bold;");

            Label confidenceLabel = new Label(String.format("(%.0f%% confidence)", tool.confidence() * 100));
            confidenceLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

            toolBox.getChildren().addAll(iconLabel, toolLabel, confidenceLabel);
            detailsContainer.getChildren().add(toolBox);

            // Show missing files if any
            if (!tool.missingRequiredFiles().isEmpty()) {
                Label missingLabel = new Label("Missing expected files: " + String.join(", ", tool.missingRequiredFiles()));
                missingLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px;");
                missingLabel.setWrapText(true);
                detailsContainer.getChildren().add(missingLabel);
            }
        }

        // Show warnings for missing mandatory types
        if (showWarnings.get()) {
            for (ProjectFileType type : ProjectFileType.values()) {
                if (FileTypeDetector.isMandatory(type) && filesByType.get(type).isEmpty()) {
                    Label warningLabel = new Label("\u26A0 Missing: " + FileTypeDetector.getDisplayName(type));
                    warningLabel.setStyle("-fx-text-fill: #ffc107; -fx-font-weight: bold;");
                    detailsContainer.getChildren().add(warningLabel);
                }
            }

            // Check for FASTA
            boolean hasFasta = files.stream().anyMatch(f -> FileTypeDetector.isFastaFile(f.getFile()));
            if (!hasFasta) {
                Label fastaWarning = new Label("\u26A0 Recommended: FASTA database file");
                fastaWarning.setStyle("-fx-text-fill: #ffc107;");
                detailsContainer.getChildren().add(fastaWarning);
            }
        }
    }

    /**
     * Get display order for file types
     */
    private List<ProjectFileType> getDisplayOrder() {
        return List.of(
                ProjectFileType.RAW,
                ProjectFileType.SEARCH,
                ProjectFileType.RESULT,
                ProjectFileType.PEAK,
                ProjectFileType.EXPERIMENTAL_DESIGN,
                ProjectFileType.OTHER
        );
    }

    /**
     * Get short name for badge
     */
    private String getShortName(ProjectFileType type) {
        return switch (type) {
            case RAW -> "RAW";
            case RESULT -> "STANDARD";
            case SEARCH -> "ANALYSIS";
            case PEAK -> "PEAK";
            case EXPERIMENTAL_DESIGN -> "SDRF";
            case MS_IMAGE_DATA -> "IMAGE";
            case GEL -> "GEL";
            case OTHER -> "OTHER";
            default -> type.name();
        };
    }

    /**
     * Create tooltip text
     */
    private String createTooltipText(ProjectFileType type, List<DataFile> typeFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append(FileTypeDetector.getDisplayName(type)).append("\n");
        sb.append("Count: ").append(typeFiles.size()).append("\n");

        if (!typeFiles.isEmpty()) {
            long size = typeFiles.stream()
                    .filter(f -> f.getFile() != null)
                    .mapToLong(f -> f.getFile().length())
                    .sum();
            sb.append("Size: ").append(formatSize(size)).append("\n\n");

            sb.append("Files:\n");
            int count = 0;
            for (DataFile df : typeFiles) {
                if (count++ >= 5) {
                    sb.append("  ... and ").append(typeFiles.size() - 5).append(" more\n");
                    break;
                }
                sb.append("  - ").append(df.getFileName()).append("\n");
            }
        } else if (FileTypeDetector.isMandatory(type)) {
            sb.append("\nRequired for submission!");
        }

        return sb.toString();
    }

    /**
     * Format file size
     */
    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    // ==================== Public API ====================

    /**
     * Set files to display
     */
    public void setFiles(Collection<DataFile> newFiles) {
        files.setAll(newFiles);
    }

    /**
     * Get files property for binding
     */
    public ObservableList<DataFile> getFiles() {
        return files;
    }

    /**
     * Get files by type
     */
    public List<DataFile> getFilesByType(ProjectFileType type) {
        return Collections.unmodifiableList(filesByType.getOrDefault(type, Collections.emptyList()));
    }

    /**
     * Get detected tool result
     */
    public FileTypeDetector.ToolDetectionResult getDetectedTool() {
        return detectedTool.get();
    }

    public ObjectProperty<FileTypeDetector.ToolDetectionResult> detectedToolProperty() {
        return detectedTool;
    }

    /**
     * Set selection handler
     */
    public void setOnTypeSelected(FileTypeSelectionHandler handler) {
        this.selectionHandler = handler;
    }

    /**
     * Show/hide details section
     */
    public void setShowDetails(boolean show) {
        showDetails.set(show);
    }

    public BooleanProperty showDetailsProperty() {
        return showDetails;
    }

    /**
     * Show/hide warnings
     */
    public void setShowWarnings(boolean show) {
        showWarnings.set(show);
    }

    public BooleanProperty showWarningsProperty() {
        return showWarnings;
    }

    /**
     * Get total file count
     */
    public int getTotalCount() {
        return totalCount.get();
    }

    public ReadOnlyIntegerProperty totalCountProperty() {
        return totalCount;
    }

    /**
     * Get total file size
     */
    public long getTotalSize() {
        return totalSize.get();
    }

    public ReadOnlyLongProperty totalSizeProperty() {
        return totalSize;
    }

    /**
     * Check if all mandatory files are present
     */
    public boolean hasAllMandatoryFiles() {
        for (ProjectFileType type : ProjectFileType.values()) {
            if (FileTypeDetector.isMandatory(type) && filesByType.get(type).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if has files of a specific type
     */
    public boolean hasFilesOfType(ProjectFileType type) {
        return !filesByType.getOrDefault(type, Collections.emptyList()).isEmpty();
    }

    /**
     * Refresh display
     */
    public void refresh() {
        updateDisplay();
    }

    // ==================== Selection Handler Interface ====================

    @FunctionalInterface
    public interface FileTypeSelectionHandler {
        void onTypeSelected(ProjectFileType type, List<DataFile> files);
    }
}
