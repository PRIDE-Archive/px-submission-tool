package uk.ac.ebi.pride.pxsubmit.view.component;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A tag/chip-style input component.
 * Users type text and press Enter (or comma) to create a chip.
 * Chips can be removed by clicking the X button.
 *
 * The component maintains a comma-separated string property for easy model binding.
 *
 * Usage:
 * <pre>
 * ChipInput keywords = new ChipInput();
 * keywords.setPromptText("Type a keyword and press Enter");
 * keywords.textProperty().bindBidirectional(model.keywordsProperty());
 * </pre>
 */
public class ChipInput extends VBox {

    private final ObservableList<String> chips = FXCollections.observableArrayList();
    private final StringProperty text = new SimpleStringProperty("");

    private final FlowPane chipContainer;
    private final TextField inputField;

    // Styles
    private static final String CHIP_STYLE =
            "-fx-background-color: #e9ecef; " +
            "-fx-background-radius: 12; " +
            "-fx-padding: 4 8 4 12; " +
            "-fx-border-color: #dee2e6; " +
            "-fx-border-radius: 12;";

    private static final String CHIP_LABEL_STYLE =
            "-fx-font-size: 12px; " +
            "-fx-text-fill: #495057;";

    private static final String CHIP_REMOVE_STYLE =
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #6c757d; " +
            "-fx-font-size: 12px; " +
            "-fx-padding: 0 0 0 6; " +
            "-fx-cursor: hand;";

    private static final String CHIP_REMOVE_HOVER_STYLE =
            "-fx-background-color: transparent; " +
            "-fx-text-fill: #dc3545; " +
            "-fx-font-size: 12px; " +
            "-fx-padding: 0 0 0 6; " +
            "-fx-cursor: hand;";

    private static final String CONTAINER_STYLE =
            "-fx-background-color: white; " +
            "-fx-border-color: #ced4da; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-padding: 8;";

    // Applied when focus leaves the field while there is typed-but-not-added text,
    // to warn the user the keyword has not been added (Enter/comma not pressed).
    private static final String CONTAINER_STYLE_PENDING =
            "-fx-background-color: #fffbe6; " +
            "-fx-border-color: #ffc107; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4; " +
            "-fx-padding: 7;";

    private final HBox mainContainer;

    private boolean updatingFromProperty = false;

    public ChipInput() {
        // Container for chips
        chipContainer = new FlowPane();
        chipContainer.setHgap(6);
        chipContainer.setVgap(6);
        chipContainer.setAlignment(Pos.CENTER_LEFT);

        // Input field
        inputField = new TextField();
        inputField.setStyle("-fx-background-color: white; -fx-border-width: 0; -fx-padding: 4;");
        inputField.setMinWidth(100);
        inputField.setPrefWidth(200);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        // Main container (chips + input)
        mainContainer = new HBox(6);
        mainContainer.setAlignment(Pos.CENTER_LEFT);
        mainContainer.setStyle(CONTAINER_STYLE);
        mainContainer.getChildren().addAll(chipContainer, inputField);

        getChildren().add(mainContainer);
        setSpacing(4);

        setupListeners();
    }

    private void setupListeners() {
        // Handle key presses in input field
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.TAB) {
                addCurrentInput();
                e.consume();
            } else if (e.getCode() == KeyCode.BACK_SPACE && inputField.getText().isEmpty() && !chips.isEmpty()) {
                // Remove last chip when backspace is pressed on empty input
                chips.remove(chips.size() - 1);
            }
        });

        // Also handle comma as separator
        inputField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.contains(",")) {
                String[] parts = newVal.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        addChip(trimmed);
                    }
                }
                inputField.clear();
            }
        });

        // Sync chips list to text property
        chips.addListener((ListChangeListener.Change<? extends String> c) -> {
            if (!updatingFromProperty) {
                updateTextProperty();
                updateChipDisplay();
            }
        });

        // Sync text property to chips list
        text.addListener((obs, oldVal, newVal) -> {
            if (!updatingFromProperty) {
                updatingFromProperty = true;
                try {
                    chips.clear();
                    if (newVal != null && !newVal.trim().isEmpty()) {
                        Arrays.stream(newVal.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .forEach(chips::add);
                    }
                    updateChipDisplay();
                } finally {
                    updatingFromProperty = false;
                }
            }
        });

        // Highlight the field when focus is lost while text has been typed but not
        // yet added as a chip (user forgot to press Enter/comma).
        inputField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                mainContainer.setStyle(CONTAINER_STYLE);
            } else {
                updatePendingHighlight();
            }
        });

        // Clear the highlight as soon as the pending text is added/removed.
        inputField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (inputField.isFocused()) {
                mainContainer.setStyle(CONTAINER_STYLE);
            } else {
                updatePendingHighlight();
            }
        });

        // Focus input when clicking on container
        setOnMouseClicked(e -> inputField.requestFocus());
    }

    /**
     * Applies the warning highlight if the input field holds typed-but-not-added
     * text while it is not focused; otherwise restores the normal style.
     */
    private void updatePendingHighlight() {
        boolean hasPendingText = inputField.getText() != null
                && !inputField.getText().trim().isEmpty();
        mainContainer.setStyle(hasPendingText && !inputField.isFocused()
                ? CONTAINER_STYLE_PENDING
                : CONTAINER_STYLE);
    }

    private void addCurrentInput() {
        String value = inputField.getText().trim();
        if (!value.isEmpty()) {
            addChip(value);
            inputField.clear();
        }
    }

    private void addChip(String value) {
        String trimmed = value.trim();
        if (!trimmed.isEmpty() && !chips.contains(trimmed)) {
            chips.add(trimmed);
        }
    }

    private void removeChip(String value) {
        chips.remove(value);
    }

    private void updateTextProperty() {
        updatingFromProperty = true;
        try {
            text.set(chips.stream().collect(Collectors.joining(", ")));
        } finally {
            updatingFromProperty = false;
        }
    }

    private void updateChipDisplay() {
        chipContainer.getChildren().clear();
        for (String chipText : chips) {
            chipContainer.getChildren().add(createChip(chipText));
        }
    }

    private HBox createChip(String chipText) {
        HBox chip = new HBox(4);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle(CHIP_STYLE);

        Label label = new Label(chipText);
        label.setStyle(CHIP_LABEL_STYLE);

        Button removeBtn = new Button("\u2715"); // Unicode X symbol
        removeBtn.setStyle(CHIP_REMOVE_STYLE);
        removeBtn.setOnMouseEntered(e -> removeBtn.setStyle(CHIP_REMOVE_HOVER_STYLE));
        removeBtn.setOnMouseExited(e -> removeBtn.setStyle(CHIP_REMOVE_STYLE));
        removeBtn.setOnAction(e -> removeChip(chipText));

        chip.getChildren().addAll(label, removeBtn);
        return chip;
    }

    // ==================== Public API ====================

    /**
     * Get the comma-separated text property.
     * Bind this to your model property.
     */
    public StringProperty textProperty() {
        return text;
    }

    /**
     * Get the current comma-separated text value.
     */
    public String getText() {
        return text.get();
    }

    /**
     * Set the comma-separated text value.
     */
    public void setText(String value) {
        text.set(value);
    }

    /**
     * Get the list of chips (read-only view).
     */
    public ObservableList<String> getChips() {
        return FXCollections.unmodifiableObservableList(chips);
    }

    /**
     * Add a chip programmatically.
     */
    public void add(String value) {
        addChip(value);
    }

    /**
     * Remove a chip programmatically.
     */
    public void remove(String value) {
        removeChip(value);
    }

    /**
     * Clear all chips.
     */
    public void clear() {
        chips.clear();
        inputField.clear();
    }

    /**
     * Set the prompt text for the input field.
     */
    public void setPromptText(String prompt) {
        inputField.setPromptText(prompt);
    }

    /**
     * Get the prompt text.
     */
    public String getPromptText() {
        return inputField.getPromptText();
    }

    /**
     * Request focus on the input field.
     */
    @Override
    public void requestFocus() {
        inputField.requestFocus();
    }

    /**
     * Check if there are any chips.
     */
    public boolean isEmpty() {
        return chips.isEmpty();
    }

    /**
     * Get the number of chips.
     */
    public int getChipCount() {
        return chips.size();
    }
}
