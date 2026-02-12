package uk.ac.ebi.pride.pxsubmit.view.component;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.Duration;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.pxsubmit.service.OlsService;
import uk.ac.ebi.pride.pxsubmit.service.OlsService.OlsOntology;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Autocomplete component for OLS (Ontology Lookup Service) term search.
 * Searches as the user types with debouncing.
 * Displays results in a dropdown with term name and accession.
 * Selected terms appear as removable chips.
 *
 * Usage:
 * <pre>
 * OlsAutocomplete speciesField = new OlsAutocomplete(OlsOntology.NCBI_TAXON);
 * speciesField.setPromptText("Search for species...");
 * speciesField.setOnTermSelected(term -> model.addSpecies(term));
 *
 * // For single selection:
 * speciesField.setMultiSelect(false);
 *
 * // With common suggestions:
 * speciesField.setCommonTerms(OlsService.getCommonSpecies());
 * </pre>
 */
public class OlsAutocomplete extends VBox {

    // Configuration
    private final OlsOntology ontology;
    private final OlsService olsService;

    // UI components
    private final TextField searchField;
    private final FlowPane chipContainer;
    private final ListView<CvParam> suggestionList;
    private final ProgressIndicator loadingIndicator;
    private final VBox suggestionsPopup;
    private final Label statusLabel;

    // Data
    private final ObservableList<CvParam> selectedTerms = FXCollections.observableArrayList();
    private final ObservableList<CvParam> suggestions = FXCollections.observableArrayList();
    private List<CvParam> commonTerms;

    // State
    private final BooleanProperty multiSelect = new SimpleBooleanProperty(true);
    private final BooleanProperty searching = new SimpleBooleanProperty(false);
    private final IntegerProperty maxResults = new SimpleIntegerProperty(10);

    // Debounce timer
    private Timer debounceTimer;
    private static final long DEBOUNCE_DELAY = 300; // ms

    // Callbacks
    private Consumer<CvParam> onTermSelected;
    private Consumer<CvParam> onTermRemoved;

    // Styles
    private static final String CHIP_STYLE =
            "-fx-background-color: #e9ecef; " +
            "-fx-background-radius: 12; " +
            "-fx-padding: 4 8 4 12; " +
            "-fx-border-color: #dee2e6; " +
            "-fx-border-radius: 12;";

    private static final String CONTAINER_STYLE =
            "-fx-background-color: white; " +
            "-fx-border-color: #ced4da; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4;";

    public OlsAutocomplete(OlsOntology ontology) {
        this.ontology = ontology;
        this.olsService = OlsService.getInstance();

        setSpacing(5);

        // Main input area
        HBox inputArea = new HBox(5);
        inputArea.setAlignment(Pos.CENTER_LEFT);
        inputArea.setStyle(CONTAINER_STYLE);
        inputArea.setPadding(new Insets(5));

        // Chip container for selected terms
        chipContainer = new FlowPane();
        chipContainer.setHgap(5);
        chipContainer.setVgap(5);
        chipContainer.setAlignment(Pos.CENTER_LEFT);

        // Search field
        searchField = new TextField();
        searchField.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-width: 0; " +
            "-fx-padding: 4; " +
            "-fx-background-insets: 0; " +
            "-fx-background-radius: 0;");
        searchField.setMinWidth(150);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // Loading indicator
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(16, 16);
        loadingIndicator.visibleProperty().bind(searching);
        loadingIndicator.managedProperty().bind(searching);

        inputArea.getChildren().addAll(chipContainer, searchField, loadingIndicator);

        // Make entire input area clickable to focus the text field
        inputArea.setOnMouseClicked(e -> searchField.requestFocus());

        // Suggestions dropdown
        suggestionsPopup = new VBox();
        suggestionsPopup.setStyle(
                "-fx-background-color: white; " +
                "-fx-border-color: #ced4da; " +
                "-fx-border-radius: 0 0 4 4; " +
                "-fx-background-radius: 0 0 4 4; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0, 0, 2);");
        suggestionsPopup.setVisible(false);
        suggestionsPopup.setManaged(false);

        // Status label (shows "Searching..." or "No results")
        statusLabel = new Label();
        statusLabel.setPadding(new Insets(8));
        statusLabel.setStyle("-fx-text-fill: #666;");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        // Suggestion list
        suggestionList = new ListView<>(suggestions);
        suggestionList.setMaxHeight(200);
        suggestionList.setCellFactory(lv -> new SuggestionCell());
        suggestionList.setPlaceholder(new Label("No results found"));

        suggestionsPopup.getChildren().addAll(statusLabel, suggestionList);

        getChildren().addAll(inputArea, suggestionsPopup);

        setupListeners();
    }

    private void setupListeners() {
        // Search on text change with debounce
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (debounceTimer != null) {
                debounceTimer.cancel();
            }

            if (newVal == null || newVal.trim().isEmpty()) {
                hideSuggestions();
                return;
            }

            if (newVal.trim().length() < 2) {
                // Show common terms for short queries
                if (commonTerms != null && !commonTerms.isEmpty()) {
                    showCommonTerms(newVal.trim().toLowerCase());
                }
                return;
            }

            debounceTimer = new Timer();
            debounceTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> search(newVal.trim()));
                }
            }, DEBOUNCE_DELAY);
        });

        // Handle key events
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN && suggestionsPopup.isVisible()) {
                suggestionList.requestFocus();
                if (!suggestions.isEmpty()) {
                    suggestionList.getSelectionModel().selectFirst();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideSuggestions();
                e.consume();
            } else if (e.getCode() == KeyCode.BACK_SPACE &&
                       searchField.getText().isEmpty() &&
                       !selectedTerms.isEmpty()) {
                // Remove last chip on backspace
                CvParam lastTerm = selectedTerms.get(selectedTerms.size() - 1);
                removeTerm(lastTerm);
                e.consume();
            }
        });

        // Handle selection from list
        suggestionList.setOnMouseClicked(e -> {
            CvParam selected = suggestionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectTerm(selected);
            }
        });

        suggestionList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                CvParam selected = suggestionList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    selectTerm(selected);
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideSuggestions();
                searchField.requestFocus();
                e.consume();
            }
        });

        // Hide suggestions when focus is lost
        searchField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && !suggestionList.isFocused()) {
                // Delay to allow click on suggestion
                Platform.runLater(() -> {
                    if (!suggestionList.isFocused()) {
                        hideSuggestions();
                    }
                });
            }
        });

        // Update chips when selectedTerms change
        selectedTerms.addListener((javafx.collections.ListChangeListener.Change<? extends CvParam> c) -> {
            updateChips();
        });
    }

    private void search(String query) {
        searching.set(true);
        showSuggestions();
        statusLabel.setText("Searching...");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);

        olsService.search(query, ontology, maxResults.get())
                .thenAccept(results -> Platform.runLater(() -> {
                    searching.set(false);
                    suggestions.setAll(results);

                    if (results.isEmpty()) {
                        statusLabel.setText("No results found for \"" + query + "\"");
                        statusLabel.setVisible(true);
                        statusLabel.setManaged(true);
                    } else {
                        statusLabel.setVisible(false);
                        statusLabel.setManaged(false);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        searching.set(false);
                        statusLabel.setText("Search failed: " + ex.getMessage());
                        statusLabel.setVisible(true);
                        statusLabel.setManaged(true);
                    });
                    return null;
                });
    }

    private void showCommonTerms(String filter) {
        if (commonTerms == null) return;

        List<CvParam> filtered = commonTerms.stream()
                .filter(t -> t.getName().toLowerCase().contains(filter))
                .limit(maxResults.get())
                .toList();

        if (!filtered.isEmpty()) {
            suggestions.setAll(filtered);
            showSuggestions();
            statusLabel.setText("Common " + ontology.getDisplayName().toLowerCase() + ":");
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
        }
    }

    private void selectTerm(CvParam term) {
        if (term == null) return;

        // Check if already selected
        boolean alreadySelected = selectedTerms.stream()
                .anyMatch(t -> t.getAccession().equals(term.getAccession()));

        if (alreadySelected) {
            hideSuggestions();
            clearSearchFieldSafely();
            return;
        }

        if (!multiSelect.get()) {
            selectedTerms.clear();
        }

        selectedTerms.add(term);
        clearSearchFieldSafely();
        hideSuggestions();

        if (onTermSelected != null) {
            onTermSelected.accept(term);
        }
    }

    /**
     * Safely clear the search field to avoid JavaFX text manipulation bugs
     */
    private void clearSearchFieldSafely() {
        try {
            if (searchField.getText() != null && !searchField.getText().isEmpty()) {
                searchField.setText("");
            }
        } catch (Exception e) {
            // Ignore text manipulation errors - known JavaFX bug
            Platform.runLater(() -> {
                try {
                    searchField.setText("");
                } catch (Exception ignored) {
                    // Still failing, just ignore
                }
            });
        }
    }

    private void removeTerm(CvParam term) {
        selectedTerms.remove(term);
        if (onTermRemoved != null) {
            onTermRemoved.accept(term);
        }
    }

    private void updateChips() {
        if (chipContainer == null) {
            return; // Guard against early listener calls
        }
        Platform.runLater(() -> {
            chipContainer.getChildren().clear();
            for (CvParam term : selectedTerms) {
                chipContainer.getChildren().add(createChip(term));
            }
        });
    }

    private HBox createChip(CvParam term) {
        HBox chip = new HBox(4);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle(CHIP_STYLE);

        Label label = new Label(term.getName());
        label.setStyle("-fx-font-size: 12px;");

        // Show accession in tooltip
        Tooltip tooltip = new Tooltip(term.getAccession() + "\n" + term.getCvLabel());
        Tooltip.install(chip, tooltip);

        Button removeBtn = new Button("\u2715");
        removeBtn.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-text-fill: #6c757d; " +
                "-fx-font-size: 10px; " +
                "-fx-padding: 0 0 0 4; " +
                "-fx-cursor: hand;");
        removeBtn.setOnAction(e -> removeTerm(term));

        chip.getChildren().addAll(label, removeBtn);
        return chip;
    }

    private void showSuggestions() {
        suggestionsPopup.setVisible(true);
        suggestionsPopup.setManaged(true);
    }

    private void hideSuggestions() {
        suggestionsPopup.setVisible(false);
        suggestionsPopup.setManaged(false);
        suggestions.clear();
    }

    /**
     * Custom cell for suggestions
     */
    private class SuggestionCell extends ListCell<CvParam> {
        @Override
        protected void updateItem(CvParam item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox box = new VBox(2);
                box.setPadding(new Insets(4));

                Label nameLabel = new Label(item.getName());
                nameLabel.setStyle("-fx-font-weight: bold;");

                Label accLabel = new Label(item.getAccession() + " (" + item.getCvLabel() + ")");
                accLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

                box.getChildren().addAll(nameLabel, accLabel);
                setGraphic(box);
            }
        }
    }

    // ==================== Public API ====================

    /**
     * Get selected terms
     */
    public ObservableList<CvParam> getSelectedTerms() {
        return selectedTerms;
    }

    /**
     * Set selected terms
     */
    public void setSelectedTerms(List<CvParam> terms) {
        selectedTerms.setAll(terms);
    }

    /**
     * Clear all selections
     */
    public void clear() {
        selectedTerms.clear();
        clearSearchFieldSafely();
        hideSuggestions();
    }

    /**
     * Set prompt text
     */
    public void setPromptText(String text) {
        searchField.setPromptText(text);
    }

    /**
     * Get prompt text
     */
    public String getPromptText() {
        return searchField.getPromptText();
    }

    /**
     * Set multi-select mode
     */
    public void setMultiSelect(boolean multi) {
        multiSelect.set(multi);
    }

    public boolean isMultiSelect() {
        return multiSelect.get();
    }

    public BooleanProperty multiSelectProperty() {
        return multiSelect;
    }

    /**
     * Set max results to show
     */
    public void setMaxResults(int max) {
        maxResults.set(max);
    }

    public int getMaxResults() {
        return maxResults.get();
    }

    /**
     * Set common terms for quick selection
     */
    public void setCommonTerms(List<CvParam> terms) {
        this.commonTerms = terms;
    }

    /**
     * Set callback for term selection
     */
    public void setOnTermSelected(Consumer<CvParam> callback) {
        this.onTermSelected = callback;
    }

    /**
     * Set callback for term removal
     */
    public void setOnTermRemoved(Consumer<CvParam> callback) {
        this.onTermRemoved = callback;
    }

    /**
     * Get the ontology this component searches
     */
    public OlsOntology getOntology() {
        return ontology;
    }

    /**
     * Check if any terms are selected
     */
    public boolean hasSelection() {
        return !selectedTerms.isEmpty();
    }

    /**
     * Programmatically add a term (used by quick select buttons)
     */
    public void addTerm(CvParam term) {
        selectTerm(term);
    }

    /**
     * Request focus on the search field
     */
    @Override
    public void requestFocus() {
        searchField.requestFocus();
    }

    /**
     * Set editable state (alternative to disable for input fields)
     */
    public void setEditable(boolean editable) {
        searchField.setEditable(editable);
        searchField.setDisable(!editable);
    }

    public boolean isEditable() {
        return searchField.isEditable();
    }
}
