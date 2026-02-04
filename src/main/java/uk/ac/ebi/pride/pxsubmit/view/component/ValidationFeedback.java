package uk.ac.ebi.pride.pxsubmit.view.component;

import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Component for displaying validation feedback with status indicators.
 * Shows errors, warnings, and success messages with icons.
 *
 * Usage:
 * <pre>
 * ValidationFeedback feedback = new ValidationFeedback();
 * feedback.addError("Title is required");
 * feedback.addWarning("Description is short");
 * feedback.setSuccess("All fields valid");
 * </pre>
 */
public class ValidationFeedback extends VBox {

    public enum MessageType {
        ERROR("\u2718", "#dc3545", "-fx-background-color: #f8d7da; -fx-border-color: #f5c6cb;"),
        WARNING("\u26A0", "#856404", "-fx-background-color: #fff3cd; -fx-border-color: #ffeeba;"),
        SUCCESS("\u2714", "#155724", "-fx-background-color: #d4edda; -fx-border-color: #c3e6cb;"),
        INFO("\u2139", "#0c5460", "-fx-background-color: #d1ecf1; -fx-border-color: #bee5eb;");

        final String icon;
        final String textColor;
        final String boxStyle;

        MessageType(String icon, String textColor, String boxStyle) {
            this.icon = icon;
            this.textColor = textColor;
            this.boxStyle = boxStyle;
        }
    }

    public record ValidationMessage(MessageType type, String message) {}

    private final List<ValidationMessage> messages = new ArrayList<>();
    private final BooleanProperty hasErrors = new SimpleBooleanProperty(false);
    private final BooleanProperty hasWarnings = new SimpleBooleanProperty(false);
    private final BooleanProperty isValid = new SimpleBooleanProperty(true);

    public ValidationFeedback() {
        setSpacing(5);
        setPadding(new Insets(5, 0, 5, 0));
    }

    /**
     * Clear all messages
     */
    public void clear() {
        messages.clear();
        getChildren().clear();
        hasErrors.set(false);
        hasWarnings.set(false);
        isValid.set(true);
    }

    /**
     * Add an error message
     */
    public void addError(String message) {
        addMessage(new ValidationMessage(MessageType.ERROR, message));
        hasErrors.set(true);
        isValid.set(false);
    }

    /**
     * Add a warning message
     */
    public void addWarning(String message) {
        addMessage(new ValidationMessage(MessageType.WARNING, message));
        hasWarnings.set(true);
    }

    /**
     * Add an info message
     */
    public void addInfo(String message) {
        addMessage(new ValidationMessage(MessageType.INFO, message));
    }

    /**
     * Set a success message (clears previous messages)
     */
    public void setSuccess(String message) {
        clear();
        addMessage(new ValidationMessage(MessageType.SUCCESS, message));
    }

    /**
     * Add a message
     */
    public void addMessage(ValidationMessage msg) {
        messages.add(msg);
        getChildren().add(createMessageBox(msg));
    }

    private HBox createMessageBox(ValidationMessage msg) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8, 12, 8, 12));
        box.setStyle(msg.type().boxStyle + " -fx-border-radius: 4; -fx-background-radius: 4;");

        Label icon = new Label(msg.type().icon);
        icon.setStyle("-fx-font-size: 14px; -fx-text-fill: " + msg.type().textColor + ";");

        Label text = new Label(msg.message());
        text.setWrapText(true);
        text.setStyle("-fx-text-fill: " + msg.type().textColor + ";");

        box.getChildren().addAll(icon, text);
        return box;
    }

    // ==================== Property Accessors ====================

    public boolean hasErrors() {
        return hasErrors.get();
    }

    public ReadOnlyBooleanProperty hasErrorsProperty() {
        return hasErrors;
    }

    public boolean hasWarnings() {
        return hasWarnings.get();
    }

    public ReadOnlyBooleanProperty hasWarningsProperty() {
        return hasWarnings;
    }

    public boolean isValid() {
        return isValid.get();
    }

    public ReadOnlyBooleanProperty validProperty() {
        return isValid;
    }

    public List<ValidationMessage> getMessages() {
        return new ArrayList<>(messages);
    }

    public int getErrorCount() {
        return (int) messages.stream()
                .filter(m -> m.type() == MessageType.ERROR)
                .count();
    }

    public int getWarningCount() {
        return (int) messages.stream()
                .filter(m -> m.type() == MessageType.WARNING)
                .count();
    }
}
