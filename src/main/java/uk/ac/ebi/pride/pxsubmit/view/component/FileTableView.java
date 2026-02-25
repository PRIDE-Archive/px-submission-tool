package uk.ac.ebi.pride.pxsubmit.view.component;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.pxsubmit.model.FileEntry;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Reusable TableView component for displaying submission files.
 * Optimized for handling thousands of files with virtualization.
 *
 * Features:
 * - Virtualized rendering for performance
 * - File type selection via ComboBox
 * - Validation status indicators
 * - Remove button per row
 * - Drag-and-drop file addition
 * - Multi-select support
 * - Sortable columns
 *
 * Usage:
 * <pre>
 * FileTableView table = new FileTableView();
 * table.setItems(model.getFiles());
 * table.setOnFilesDropped(files -> addFiles(files));
 * table.setOnFileRemoved(file -> removeFile(file));
 * </pre>
 */
public class FileTableView extends TableView<DataFile> {

    private static final Logger logger = LoggerFactory.getLogger(FileTableView.class);

    // Cell size for virtualization
    private static final double FIXED_CELL_SIZE = 32;

    // Pagination thresholds
    private static final int PAGINATION_THRESHOLD = 1000; // Enable pagination above this
    private static final int DEFAULT_PAGE_SIZE = 100;

    // File size formatter
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,##0.#");

    // Event handlers
    private Consumer<List<File>> onFilesDropped;
    private Consumer<DataFile> onFileRemoved;
    private Consumer<DataFile> onFileTypeChanged;

    // Master list and filtered/paginated views
    private ObservableList<DataFile> masterList;
    private FilteredList<DataFile> filteredList;
    private final ObjectProperty<Predicate<DataFile>> filterPredicate = new SimpleObjectProperty<>(f -> true);
    private final StringProperty searchText = new SimpleStringProperty("");

    // Checksum lookup for search
    private Function<DataFile, String> checksumLookup;

    // Pagination state
    private final BooleanProperty paginationEnabled = new SimpleBooleanProperty(false);
    private final IntegerProperty currentPage = new SimpleIntegerProperty(0);
    private final IntegerProperty pageSize = new SimpleIntegerProperty(DEFAULT_PAGE_SIZE);
    private final IntegerProperty totalPages = new SimpleIntegerProperty(1);
    private final IntegerProperty totalItems = new SimpleIntegerProperty(0);

    // Columns
    private TableColumn<DataFile, String> nameColumn;
    private TableColumn<DataFile, String> pathColumn;
    private TableColumn<DataFile, Long> sizeColumn;
    private TableColumn<DataFile, ProjectFileType> typeColumn;
    private TableColumn<DataFile, DataFile> statusColumn;
    private TableColumn<DataFile, DataFile> actionsColumn;

    public FileTableView() {
        initialize();
    }

    private void initialize() {
        // Enable virtualization
        setFixedCellSize(FIXED_CELL_SIZE);

        // Multi-select
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Editable for type column
        setEditable(true);

        // Fill available width
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setMaxWidth(Double.MAX_VALUE);

        // Placeholder
        setPlaceholder(createPlaceholder());

        // Create columns
        createColumns();

        // Setup drag and drop
        setupDragAndDrop();

        // Style class
        getStyleClass().add("file-table-view");
    }

    private void createColumns() {
        // File name column
        nameColumn = new TableColumn<>("File Name");
        nameColumn.setCellValueFactory(param ->
            new ReadOnlyObjectWrapper<>(param.getValue().getFileName()));
        nameColumn.setPrefWidth(250);
        nameColumn.setMinWidth(150);

        // Path column
        pathColumn = new TableColumn<>("Path");
        pathColumn.setCellValueFactory(param ->
            new ReadOnlyObjectWrapper<>(param.getValue().getFilePath()));
        pathColumn.setPrefWidth(300);
        pathColumn.setMinWidth(100);

        // Size column
        sizeColumn = new TableColumn<>("Size");
        sizeColumn.setCellValueFactory(param -> {
            File file = param.getValue().getFile();
            return new ReadOnlyObjectWrapper<>(file != null ? file.length() : 0L);
        });
        sizeColumn.setCellFactory(col -> new FileSizeCell());
        sizeColumn.setPrefWidth(100);
        sizeColumn.setMinWidth(80);
        sizeColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Type column (editable)
        typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(param ->
            new ReadOnlyObjectWrapper<>(param.getValue().getFileType()));
        typeColumn.setCellFactory(ComboBoxTableCell.forTableColumn(ProjectFileType.values()));
        typeColumn.setOnEditCommit(event -> {
            DataFile dataFile = event.getRowValue();
            ProjectFileType newType = event.getNewValue();
            dataFile.setFileType(newType);
            logger.debug("Changed file type for {}: {}", dataFile.getFileName(), newType);
            if (onFileTypeChanged != null) {
                onFileTypeChanged.accept(dataFile);
            }
        });
        typeColumn.setPrefWidth(150);
        typeColumn.setMinWidth(120);
        typeColumn.setEditable(true);

        // Status column
        statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(param ->
            new ReadOnlyObjectWrapper<>(param.getValue()));
        statusColumn.setCellFactory(col -> new ValidationStatusCell());
        statusColumn.setPrefWidth(80);
        statusColumn.setMinWidth(60);
        statusColumn.setSortable(false);

        // Actions column (remove button)
        actionsColumn = new TableColumn<>("");
        actionsColumn.setCellValueFactory(param ->
            new ReadOnlyObjectWrapper<>(param.getValue()));
        actionsColumn.setCellFactory(col -> new RemoveButtonCell());
        actionsColumn.setPrefWidth(50);
        actionsColumn.setMinWidth(40);
        actionsColumn.setMaxWidth(50);
        actionsColumn.setSortable(false);

        getColumns().addAll(nameColumn, pathColumn, sizeColumn, typeColumn, statusColumn, actionsColumn);
    }

    private Label createPlaceholder() {
        Label placeholder = new Label("Drop files here or click 'Add Files' to select files");
        placeholder.setStyle("-fx-text-fill: #999; -fx-font-size: 14px;");
        return placeholder;
    }

    private void setupDragAndDrop() {
        setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                getStyleClass().add("drag-over");
            }
        });

        setOnDragExited(event -> {
            getStyleClass().remove("drag-over");
        });

        setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && onFilesDropped != null) {
                onFilesDropped.accept(db.getFiles());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
            getStyleClass().remove("drag-over");
        });
    }

    // ==================== Custom Cells ====================

    /**
     * Cell for displaying file size in human-readable format
     */
    private static class FileSizeCell extends TableCell<DataFile, Long> {
        @Override
        protected void updateItem(Long size, boolean empty) {
            super.updateItem(size, empty);
            if (empty || size == null) {
                setText(null);
            } else {
                setText(formatFileSize(size));
            }
        }

        private String formatFileSize(long size) {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return SIZE_FORMAT.format(size / 1024.0) + " KB";
            if (size < 1024 * 1024 * 1024) return SIZE_FORMAT.format(size / (1024.0 * 1024)) + " MB";
            return SIZE_FORMAT.format(size / (1024.0 * 1024 * 1024)) + " GB";
        }
    }

    /**
     * Cell for displaying validation status with icon
     */
    private class ValidationStatusCell extends TableCell<DataFile, DataFile> {
        private final ImageView iconView = new ImageView();

        public ValidationStatusCell() {
            iconView.setFitWidth(16);
            iconView.setFitHeight(16);
            setAlignment(Pos.CENTER);
        }

        @Override
        protected void updateItem(DataFile dataFile, boolean empty) {
            super.updateItem(dataFile, empty);
            if (empty || dataFile == null) {
                setGraphic(null);
                setText(null);
                setTooltip(null);
            } else {
                // Check file validity
                File file = dataFile.getFile();
                if (file == null || !file.exists()) {
                    setStyle("-fx-text-fill: #dc3545;");
                    setText("!");
                    setTooltip(new Tooltip("File not found"));
                } else if (!file.canRead()) {
                    setStyle("-fx-text-fill: #ffc107;");
                    setText("!");
                    setTooltip(new Tooltip("File not readable"));
                } else {
                    setStyle("-fx-text-fill: #28a745;");
                    setText("\u2713"); // Check mark
                    setTooltip(new Tooltip("File OK"));
                }
            }
        }
    }

    /**
     * Cell with remove button
     */
    private class RemoveButtonCell extends TableCell<DataFile, DataFile> {
        private final Button removeButton = new Button("×");

        public RemoveButtonCell() {
            removeButton.getStyleClass().add("remove-button");
            removeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; " +
                "-fx-font-size: 16px; -fx-padding: 0; -fx-cursor: hand;");
            removeButton.setOnAction(event -> {
                DataFile dataFile = getItem();
                if (dataFile != null && onFileRemoved != null) {
                    onFileRemoved.accept(dataFile);
                }
            });

            // Hover effect
            removeButton.setOnMouseEntered(e ->
                removeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #dc3545; " +
                    "-fx-font-size: 16px; -fx-padding: 0; -fx-cursor: hand;"));
            removeButton.setOnMouseExited(e ->
                removeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; " +
                    "-fx-font-size: 16px; -fx-padding: 0; -fx-cursor: hand;"));
        }

        @Override
        protected void updateItem(DataFile dataFile, boolean empty) {
            super.updateItem(dataFile, empty);
            if (empty || dataFile == null) {
                setGraphic(null);
            } else {
                // Don't allow removing checksum.txt
                if ("checksum.txt".equals(dataFile.getFileName())) {
                    setGraphic(null);
                } else {
                    setGraphic(removeButton);
                }
            }
        }
    }

    // ==================== Event Handlers ====================

    public void setOnFilesDropped(Consumer<List<File>> handler) {
        this.onFilesDropped = handler;
    }

    public void setOnFileRemoved(Consumer<DataFile> handler) {
        this.onFileRemoved = handler;
    }

    public void setOnFileTypeChanged(Consumer<DataFile> handler) {
        this.onFileTypeChanged = handler;
    }

    /**
     * Set a function to look up checksums for files (used in search filtering).
     */
    public void setChecksumLookup(Function<DataFile, String> lookup) {
        this.checksumLookup = lookup;
    }

    // ==================== Utility Methods ====================

    /**
     * Get selected files
     */
    public List<DataFile> getSelectedFiles() {
        return getSelectionModel().getSelectedItems();
    }

    /**
     * Select all files
     */
    public void selectAll() {
        getSelectionModel().selectAll();
    }

    /**
     * Clear selection
     */
    public void clearSelection() {
        getSelectionModel().clearSelection();
    }

    /**
     * Get file count summary
     */
    public String getFileSummary() {
        ObservableList<DataFile> items = getItems();
        if (items == null || items.isEmpty()) {
            return "No files";
        }

        long totalSize = items.stream()
            .filter(f -> f.getFile() != null)
            .mapToLong(f -> f.getFile().length())
            .sum();

        return String.format("%d files (%s)", items.size(), formatTotalSize(totalSize));
    }

    private String formatTotalSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return SIZE_FORMAT.format(size / 1024.0) + " KB";
        if (size < 1024 * 1024 * 1024) return SIZE_FORMAT.format(size / (1024.0 * 1024)) + " MB";
        return SIZE_FORMAT.format(size / (1024.0 * 1024 * 1024)) + " GB";
    }

    /**
     * Scroll to specific file
     */
    public void scrollTo(DataFile dataFile) {
        int index = getItems().indexOf(dataFile);
        if (index >= 0) {
            scrollTo(index);
            getSelectionModel().select(index);
        }
    }

    /**
     * Show/hide path column
     */
    public void setPathColumnVisible(boolean visible) {
        pathColumn.setVisible(visible);
    }

    /**
     * Show/hide status column
     */
    public void setStatusColumnVisible(boolean visible) {
        statusColumn.setVisible(visible);
    }

    // ==================== Pagination & Filtering ====================

    /**
     * Set items with automatic pagination for large datasets.
     * Named setDataFiles to avoid conflict with final TableView.setItems method.
     */
    public void setDataFiles(ObservableList<DataFile> items) {
        this.masterList = items;

        // Create filtered list
        filteredList = new FilteredList<>(items, f -> true);

        // Bind filter predicate
        filteredList.predicateProperty().bind(
                Bindings.createObjectBinding(() -> {
                    Predicate<DataFile> textFilter = createTextFilter(searchText.get());
                    Predicate<DataFile> customFilter = filterPredicate.get();
                    return textFilter.and(customFilter != null ? customFilter : f -> true);
                }, searchText, filterPredicate)
        );

        // Track total items
        filteredList.addListener((ListChangeListener<DataFile>) c -> {
            totalItems.set(filteredList.size());
            updatePagination();
        });

        totalItems.set(items.size());
        updatePagination();

        // Check if pagination is needed
        if (items.size() > PAGINATION_THRESHOLD) {
            paginationEnabled.set(true);
            applyPagination();
        } else {
            paginationEnabled.set(false);
            super.setItems(filteredList);
        }
    }

    /**
     * Create text filter predicate
     */
    private Predicate<DataFile> createTextFilter(String text) {
        if (text == null || text.trim().isEmpty()) {
            return f -> true;
        }

        String lowerText = text.toLowerCase().trim();
        return file -> {
            if (file.getFileName() != null && file.getFileName().toLowerCase().contains(lowerText)) {
                return true;
            }
            if (file.getFilePath() != null && file.getFilePath().toLowerCase().contains(lowerText)) {
                return true;
            }
            if (file.getFileType() != null && file.getFileType().name().toLowerCase().contains(lowerText)) {
                return true;
            }
            if (checksumLookup != null) {
                String checksum = checksumLookup.apply(file);
                if (checksum != null && checksum.toLowerCase().contains(lowerText)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Update pagination state
     */
    private void updatePagination() {
        int total = filteredList.size();
        int pages = Math.max(1, (int) Math.ceil((double) total / pageSize.get()));
        totalPages.set(pages);

        // Adjust current page if needed
        if (currentPage.get() >= pages) {
            currentPage.set(Math.max(0, pages - 1));
        }

        if (paginationEnabled.get()) {
            applyPagination();
        }
    }

    /**
     * Apply pagination to table
     */
    private void applyPagination() {
        int start = currentPage.get() * pageSize.get();
        int end = Math.min(start + pageSize.get(), filteredList.size());

        ObservableList<DataFile> pageItems = FXCollections.observableArrayList();
        if (start < filteredList.size()) {
            pageItems.addAll(filteredList.subList(start, end));
        }

        super.setItems(pageItems);
    }

    /**
     * Go to specific page
     */
    public void goToPage(int page) {
        if (page >= 0 && page < totalPages.get()) {
            currentPage.set(page);
            if (paginationEnabled.get()) {
                applyPagination();
            }
        }
    }

    /**
     * Go to next page
     */
    public void nextPage() {
        goToPage(currentPage.get() + 1);
    }

    /**
     * Go to previous page
     */
    public void previousPage() {
        goToPage(currentPage.get() - 1);
    }

    /**
     * Go to first page
     */
    public void firstPage() {
        goToPage(0);
    }

    /**
     * Go to last page
     */
    public void lastPage() {
        goToPage(totalPages.get() - 1);
    }

    /**
     * Set search/filter text
     */
    public void setSearchText(String text) {
        searchText.set(text);
        currentPage.set(0);
        updatePagination();
    }

    /**
     * Filter files by name (delegates to search text)
     */
    public void filterByName(String text) {
        setSearchText(text != null ? text : "");
    }

    /**
     * Set custom filter predicate
     */
    public void setFilterPredicate(Predicate<DataFile> predicate) {
        filterPredicate.set(predicate);
        currentPage.set(0);
        updatePagination();
    }

    /**
     * Filter by file type
     */
    public void filterByType(ProjectFileType type) {
        if (type == null) {
            filterPredicate.set(f -> true);
        } else {
            filterPredicate.set(f -> f.getFileType() == type);
        }
        currentPage.set(0);
        updatePagination();
    }

    /**
     * Clear all filters
     */
    public void clearFilters() {
        searchText.set("");
        filterPredicate.set(f -> true);
        currentPage.set(0);
        updatePagination();
    }

    /**
     * Get pagination controls as an HBox
     */
    public HBox createPaginationControls() {
        HBox controls = new HBox(8);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(8, 0, 8, 0));

        Button firstBtn = new Button("\u00AB"); // <<
        firstBtn.setOnAction(e -> firstPage());
        firstBtn.disableProperty().bind(currentPage.isEqualTo(0));

        Button prevBtn = new Button("\u2039"); // <
        prevBtn.setOnAction(e -> previousPage());
        prevBtn.disableProperty().bind(currentPage.isEqualTo(0));

        Label pageLabel = new Label();
        pageLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("Page %d of %d", currentPage.get() + 1, totalPages.get()),
                currentPage, totalPages));

        Button nextBtn = new Button("\u203A"); // >
        nextBtn.setOnAction(e -> nextPage());
        nextBtn.disableProperty().bind(currentPage.greaterThanOrEqualTo(totalPages.subtract(1)));

        Button lastBtn = new Button("\u00BB"); // >>
        lastBtn.setOnAction(e -> lastPage());
        lastBtn.disableProperty().bind(currentPage.greaterThanOrEqualTo(totalPages.subtract(1)));

        // Total items label
        Label totalLabel = new Label();
        totalLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("(%,d items)", totalItems.get()),
                totalItems));
        totalLabel.setStyle("-fx-text-fill: #666;");

        controls.getChildren().addAll(firstBtn, prevBtn, pageLabel, nextBtn, lastBtn, totalLabel);

        // Only show when pagination is enabled
        controls.visibleProperty().bind(paginationEnabled);
        controls.managedProperty().bind(paginationEnabled);

        return controls;
    }

    /**
     * Create search field
     */
    public TextField createSearchField() {
        TextField searchField = new TextField();
        searchField.setPromptText("Search files...");
        searchField.setPrefWidth(200);

        // Debounce search to avoid too many updates
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            setSearchText(newVal);
        });

        return searchField;
    }

    /**
     * Create type filter combo box
     */
    public ComboBox<ProjectFileType> createTypeFilter() {
        ComboBox<ProjectFileType> typeFilter = new ComboBox<>();
        typeFilter.getItems().add(null); // "All" option
        typeFilter.getItems().addAll(ProjectFileType.values());
        typeFilter.setPromptText("All Types");

        typeFilter.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ProjectFileType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All Types" : item.name());
            }
        });

        typeFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ProjectFileType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All Types" : item.name());
            }
        });

        typeFilter.setOnAction(e -> filterByType(typeFilter.getValue()));

        return typeFilter;
    }

    /**
     * Create a complete toolbar with search, filter, and pagination
     */
    public BorderPane createToolbar() {
        BorderPane toolbar = new BorderPane();
        toolbar.setPadding(new Insets(8));
        toolbar.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-width: 0 0 1 0;");

        // Left: Search and filter
        HBox leftBox = new HBox(10);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        leftBox.getChildren().addAll(createSearchField(), createTypeFilter());

        // Right: Summary
        Label summaryLabel = new Label();
        summaryLabel.textProperty().bind(Bindings.createStringBinding(
                () -> getFileSummary(),
                totalItems));
        summaryLabel.setStyle("-fx-text-fill: #666;");

        toolbar.setLeft(leftBox);
        toolbar.setRight(summaryLabel);

        return toolbar;
    }

    // ==================== Property Accessors ====================

    public BooleanProperty paginationEnabledProperty() {
        return paginationEnabled;
    }

    public IntegerProperty currentPageProperty() {
        return currentPage;
    }

    public IntegerProperty totalPagesProperty() {
        return totalPages;
    }

    public IntegerProperty totalItemsProperty() {
        return totalItems;
    }

    public IntegerProperty pageSizeProperty() {
        return pageSize;
    }

    public StringProperty searchTextProperty() {
        return searchText;
    }

    /**
     * Set page size
     */
    public void setPageSize(int size) {
        pageSize.set(size);
        updatePagination();
    }

    /**
     * Enable/disable pagination manually
     */
    public void setPaginationEnabled(boolean enabled) {
        paginationEnabled.set(enabled);
        if (enabled && masterList != null) {
            applyPagination();
        } else if (!enabled && filteredList != null) {
            super.setItems(filteredList);
        }
    }
}
