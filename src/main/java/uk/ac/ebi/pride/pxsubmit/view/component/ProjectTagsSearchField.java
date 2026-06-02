package uk.ac.ebi.pride.pxsubmit.view.component;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.css.PseudoClass;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Popup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Searchable multi-select project tags picker: text field with a popup list (same UX as
 * {@link CountrySearchField}), plus removable chips for selected tags.
 */
public class ProjectTagsSearchField extends VBox {

    private static final double POPUP_MAX_HEIGHT = 220;
    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");

    private final TextField searchField;
    private final Button dropdownButton;
    private final ListView<String> suggestionList;
    private final ObservableList<String> suggestions = FXCollections.observableArrayList();
    private final Popup dropdownPopup;
    private final FlowPane chipsPane;
    private final String[] allTags;
    private final Set<String> selectedTags = new LinkedHashSet<>();
    private boolean updating;
    private Consumer<Set<String>> onSelectionChanged;

    public ProjectTagsSearchField(String[] tags) {
        this.allTags = tags.clone();
        setSpacing(6);

        HBox searchRow = new HBox();
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setSpacing(0);
        searchRow.getStyleClass().add("form-dropdown");
        searchRow.setMaxWidth(400);
        searchRow.setPrefWidth(400);

        searchField = new TextField();
        searchField.getStyleClass().add("country-search-field");
        searchField.setPromptText("Search project tags...");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        dropdownButton = createDropdownButton();
        dropdownButton.setOnAction(e -> toggleDropdown());

        suggestionList = new ListView<>(suggestions);
        suggestionList.setPrefHeight(POPUP_MAX_HEIGHT);
        suggestionList.setMaxHeight(POPUP_MAX_HEIGHT);
        suggestionList.setCellFactory(list -> new TagListCell());
        Label emptyLabel = new Label("No matching tags");
        emptyLabel.setPadding(new Insets(8));
        emptyLabel.setStyle("-fx-text-fill: #666;");
        suggestionList.setPlaceholder(emptyLabel);

        VBox popupRoot = new VBox(suggestionList);
        popupRoot.getStyleClass().add("form-dropdown-popup");

        dropdownPopup = new Popup();
        dropdownPopup.setAutoHide(true);
        dropdownPopup.getContent().add(popupRoot);

        chipsPane = new FlowPane();
        chipsPane.setHgap(6);
        chipsPane.setVgap(4);
        chipsPane.setPadding(new Insets(2, 0, 0, 0));

        searchField.focusedProperty().addListener((obs, wasFocused, focused) ->
                searchRow.pseudoClassStateChanged(FOCUSED, focused));

        searchField.textProperty().addListener((obs, oldText, newText) -> {
            if (updating) {
                return;
            }
            updateSuggestions(newText, true);
        });

        searchField.setOnMouseClicked(e -> {
            if (!dropdownPopup.isShowing()) {
                updateSuggestions(searchField.getText(), true);
            }
        });

        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                if (!dropdownPopup.isShowing()) {
                    updateSuggestions(searchField.getText(), true);
                }
                if (!suggestions.isEmpty()) {
                    suggestionList.requestFocus();
                    suggestionList.getSelectionModel().selectFirst();
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideDropdown();
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER && dropdownPopup.isShowing()
                    && !suggestions.isEmpty()) {
                toggleTag(suggestions.get(
                        Math.max(0, suggestionList.getSelectionModel().getSelectedIndex())));
                e.consume();
            }
        });

        suggestionList.setOnMouseClicked(e -> {
            String selected = suggestionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                toggleTag(selected);
            }
        });

        suggestionList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                String selected = suggestionList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    toggleTag(selected);
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideDropdown();
                searchField.requestFocus();
                e.consume();
            }
        });

        dropdownPopup.setOnHidden(e -> suggestionList.getSelectionModel().clearSelection());

        searchRow.getChildren().addAll(searchField, dropdownButton);
        getChildren().addAll(searchRow, chipsPane);
    }

    public void setOnSelectionChanged(Consumer<Set<String>> handler) {
        this.onSelectionChanged = handler;
    }

    public TextField getSearchField() {
        return searchField;
    }

    public List<String> getSelectedTags() {
        return new ArrayList<>(selectedTags);
    }

    public void setSelectedTags(Collection<String> tags) {
        selectedTags.clear();
        if (tags != null) {
            for (String tag : tags) {
                if (tag != null && !tag.isBlank()) {
                    resolveTagName(tag).ifPresent(selectedTags::add);
                }
            }
        }
        refreshChips();
        suggestionList.refresh();
    }

    private void toggleTag(String tag) {
        String resolved = resolveTagName(tag).orElse(null);
        if (resolved == null) {
            return;
        }
        if (selectedTags.contains(resolved)) {
            selectedTags.remove(resolved);
        } else {
            selectedTags.add(resolved);
        }
        refreshChips();
        suggestionList.refresh();
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(Set.copyOf(selectedTags));
        }
    }

    private void refreshChips() {
        chipsPane.getChildren().clear();
        for (String tag : selectedTags) {
            Label chip = new Label(tag + "  \u2715");
            chip.setStyle(
                    "-fx-background-color: #0066cc; " +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 3 8; " +
                    "-fx-background-radius: 12; " +
                    "-fx-font-size: 11px; " +
                    "-fx-cursor: hand;");
            chip.setOnMouseClicked(e -> {
                selectedTags.remove(tag);
                refreshChips();
                suggestionList.refresh();
                if (onSelectionChanged != null) {
                    onSelectionChanged.accept(Set.copyOf(selectedTags));
                }
            });
            chipsPane.getChildren().add(chip);
        }
    }

    private void toggleDropdown() {
        if (dropdownPopup.isShowing()) {
            hideDropdown();
        } else {
            String text = searchField.getText();
            if (text == null || text.trim().isEmpty()) {
                showAllTags();
            } else {
                updateSuggestions(text, true);
            }
        }
    }

    private void showAllTags() {
        suggestions.setAll(Arrays.asList(allTags));
        showDropdown();
    }

    private void updateSuggestions(String text, boolean openDropdown) {
        String filter = text == null ? "" : text.trim().toLowerCase();
        if (filter.isEmpty()) {
            if (openDropdown) {
                showAllTags();
            } else {
                hideDropdown();
            }
            return;
        }
        suggestions.setAll(Arrays.stream(allTags)
                .filter(tag -> tag.toLowerCase().startsWith(filter))
                .toList());
        if (openDropdown) {
            showDropdown();
        } else if (suggestions.isEmpty()) {
            hideDropdown();
        }
    }

    private void showDropdown() {
        double width = searchField.getParent().getBoundsInLocal().getWidth();
        if (width <= 0) {
            width = 400;
        }
        suggestionList.setPrefWidth(width);
        Bounds screenBounds = searchField.getParent().localToScreen(
                searchField.getParent().getBoundsInLocal());
        if (screenBounds != null) {
            dropdownPopup.show(searchField.getParent(), screenBounds.getMinX(), screenBounds.getMaxY());
        } else {
            Platform.runLater(() -> {
                Bounds retry = searchField.getParent().localToScreen(
                        searchField.getParent().getBoundsInLocal());
                if (retry != null) {
                    dropdownPopup.show(searchField.getParent(), retry.getMinX(), retry.getMaxY());
                }
            });
        }
    }

    private void hideDropdown() {
        dropdownPopup.hide();
    }

    private java.util.Optional<String> resolveTagName(String value) {
        if (value == null || value.isBlank()) {
            return java.util.Optional.empty();
        }
        String trimmed = value.trim();
        for (String tag : allTags) {
            if (tag.equalsIgnoreCase(trimmed)) {
                return java.util.Optional.of(tag);
            }
        }
        return java.util.Optional.empty();
    }

    private static Button createDropdownButton() {
        Button button = new Button();
        button.setGraphic(createChevronDownGraphic());
        button.getStyleClass().add("country-dropdown-arrow");
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setFocusTraversable(false);
        button.setMinWidth(32);
        button.setPrefWidth(32);
        return button;
    }

    private static Node createChevronDownGraphic() {
        Polygon triangle = new Polygon(0, 0, 10, 0, 5, 6);
        triangle.setFill(Color.web("#495057"));
        StackPane graphic = new StackPane(triangle);
        graphic.setMaxSize(10, 6);
        return graphic;
    }

    private class TagListCell extends ListCell<String> {
        private final CheckBox checkBox = new CheckBox();

        TagListCell() {
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setGraphic(checkBox);
            checkBox.setMouseTransparent(true);
        }

        @Override
        protected void updateItem(String tag, boolean empty) {
            super.updateItem(tag, empty);
            if (empty || tag == null) {
                checkBox.setText(null);
                checkBox.setSelected(false);
                setGraphic(null);
            } else {
                checkBox.setText(tag);
                checkBox.setSelected(selectedTags.contains(tag));
                setGraphic(checkBox);
            }
        }
    }
}
