package uk.ac.ebi.pride.pxsubmit.view.component;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.css.PseudoClass;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Popup;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Searchable country dropdown: text field with a popup list (combo-style UX).
 * Filters by substring (case-insensitive) as the user types; avoids editable ComboBox item-mutation loops.
 */
public class CountrySearchField extends HBox {

    private static final double POPUP_MAX_HEIGHT = 200;
    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");

    private final TextField searchField;
    private final Button dropdownButton;
    private final ListView<String> suggestionList;
    private final ObservableList<String> suggestions = FXCollections.observableArrayList();
    private final Popup dropdownPopup;
    private final String[] allCountries;
    private boolean updating;
    private Consumer<String> onCountrySelected;

    public CountrySearchField(String[] countries) {
        this.allCountries = countries.clone();
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(0);
        getStyleClass().add("form-dropdown");

        searchField = new TextField();
        searchField.getStyleClass().add("country-search-field");
        searchField.setPromptText("Select or type country (e.g. Uni, United…)");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        dropdownButton = createDropdownButton();
        dropdownButton.setOnAction(e -> toggleDropdown());

        suggestionList = new ListView<>(suggestions);
        suggestionList.setPrefHeight(POPUP_MAX_HEIGHT);
        suggestionList.setMaxHeight(POPUP_MAX_HEIGHT);
        Label emptyLabel = new Label("No matching country");
        emptyLabel.setPadding(new Insets(8));
        emptyLabel.setStyle("-fx-text-fill: #666;");
        suggestionList.setPlaceholder(emptyLabel);

        VBox popupRoot = new VBox(suggestionList);
        popupRoot.getStyleClass().add("form-dropdown-popup");

        dropdownPopup = new Popup();
        dropdownPopup.setAutoHide(true);
        dropdownPopup.getContent().add(popupRoot);

        searchField.focusedProperty().addListener((obs, wasFocused, focused) ->
                pseudoClassStateChanged(FOCUSED, focused));

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
            } else if (e.getCode() == KeyCode.ENTER && dropdownPopup.isShowing() && !suggestions.isEmpty()) {
                selectCountry(suggestions.get(0));
                e.consume();
            }
        });

        suggestionList.setOnMouseClicked(e -> {
            String selected = suggestionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectCountry(selected);
            }
        });

        suggestionList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String selected = suggestionList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    selectCountry(selected);
                }
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                hideDropdown();
                searchField.requestFocus();
                e.consume();
            }
        });

        dropdownPopup.setOnHidden(e -> suggestionList.getSelectionModel().clearSelection());

        getChildren().addAll(searchField, dropdownButton);
        setMaxWidth(400);
        setPrefWidth(400);
    }

    public void setOnCountrySelected(Consumer<String> handler) {
        this.onCountrySelected = handler;
    }

    public TextField getSearchField() {
        return searchField;
    }

    public String getText() {
        return searchField.getText();
    }

    public void setText(String text) {
        updating = true;
        try {
            searchField.setText(text == null ? "" : text);
            hideDropdown();
        } finally {
            updating = false;
        }
    }

    /** Canonical country name if text matches the list exactly, otherwise null. */
    public String getResolvedCountry() {
        return resolveCountryName(searchField.getText());
    }

    public void selectCountry(String country) {
        String resolved = resolveCountryName(country);
        if (resolved == null) {
            return;
        }
        updating = true;
        try {
            searchField.setText(resolved);
            hideDropdown();
        } finally {
            updating = false;
        }
        if (onCountrySelected != null) {
            onCountrySelected.accept(resolved);
        }
    }

    private void toggleDropdown() {
        if (dropdownPopup.isShowing()) {
            hideDropdown();
        } else {
            String text = searchField.getText();
            if (text == null || text.trim().isEmpty()) {
                showAllCountries();
            } else {
                updateSuggestions(text, true);
            }
        }
    }

    private void showAllCountries() {
        suggestions.setAll(Arrays.asList(allCountries));
        showDropdown();
    }

    private void updateSuggestions(String text, boolean openDropdown) {
        String filter = text == null ? "" : text.trim().toLowerCase();
        if (filter.isEmpty()) {
            if (openDropdown) {
                showAllCountries();
            } else {
                hideDropdown();
            }
            return;
        }
        String exact = resolveCountryName(text);
        if (exact != null) {
            hideDropdown();
            if (onCountrySelected != null) {
                onCountrySelected.accept(exact);
            }
            return;
        }
        suggestions.setAll(Arrays.stream(allCountries)
                .filter(country -> country.toLowerCase().contains(filter))
                .toList());
        if (openDropdown) {
            showDropdown();
        } else if (suggestions.isEmpty()) {
            hideDropdown();
        }
    }

    private void showDropdown() {
        double width = getWidth() > 0 ? getWidth() : searchField.getPrefWidth() + dropdownButton.getPrefWidth();
        suggestionList.setPrefWidth(width);
        Bounds screenBounds = localToScreen(getBoundsInLocal());
        if (screenBounds != null) {
            dropdownPopup.show(this, screenBounds.getMinX(), screenBounds.getMaxY());
        } else {
            Platform.runLater(() -> {
                Bounds retry = localToScreen(getBoundsInLocal());
                if (retry != null) {
                    dropdownPopup.show(this, retry.getMinX(), retry.getMaxY());
                }
            });
        }
    }

    private void hideDropdown() {
        dropdownPopup.hide();
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

    /** Small triangle arrow (font-independent). */
    private static Node createChevronDownGraphic() {
        Polygon triangle = new Polygon(0, 0, 10, 0, 5, 6);
        triangle.setFill(Color.web("#495057"));
        StackPane graphic = new StackPane(triangle);
        graphic.setMaxSize(10, 6);
        return graphic;
    }

    private String resolveCountryName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (String country : allCountries) {
            if (country.equalsIgnoreCase(trimmed)) {
                return country;
            }
        }
        return null;
    }
}
