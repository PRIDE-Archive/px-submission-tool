package uk.ac.ebi.pride.pxsubmit.view.component;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * A notification banner component for displaying messages to users.
 *
 * Features:
 * - Multiple notification types (info, success, warning, error)
 * - Auto-dismiss with configurable duration
 * - Manual dismiss button
 * - Smooth fade animations
 * - Stackable notifications
 */
public class NotificationPane extends VBox {

    private static final Duration DEFAULT_AUTO_DISMISS = Duration.seconds(5);
    private static final Duration FADE_DURATION = Duration.millis(300);

    private final BooleanProperty showing = new SimpleBooleanProperty(false);

    public NotificationPane() {
        setAlignment(Pos.TOP_CENTER);
        setSpacing(8);
        setPadding(new Insets(10));
        setPickOnBounds(false); // Allow click-through when empty
    }

    /**
     * Notification type
     */
    public enum NotificationType {
        INFO("info-box", "info"),
        SUCCESS("success-box", "check-circle"),
        WARNING("warning-box", "alert-triangle"),
        ERROR("error-box", "x-circle");

        private final String styleClass;
        private final String icon;

        NotificationType(String styleClass, String icon) {
            this.styleClass = styleClass;
            this.icon = icon;
        }

        public String getStyleClass() {
            return styleClass;
        }

        public String getIcon() {
            return icon;
        }
    }

    /**
     * Show a notification
     */
    public void show(String message, NotificationType type) {
        show(message, type, DEFAULT_AUTO_DISMISS);
    }

    /**
     * Show a notification with custom duration
     */
    public void show(String message, NotificationType type, Duration autoDismiss) {
        HBox notification = createNotification(message, type, autoDismiss);

        // Fade in
        notification.setOpacity(0);
        getChildren().add(notification);
        showing.set(true);

        FadeTransition fadeIn = new FadeTransition(FADE_DURATION, notification);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    /**
     * Show info notification
     */
    public void showInfo(String message) {
        show(message, NotificationType.INFO);
    }

    /**
     * Show success notification
     */
    public void showSuccess(String message) {
        show(message, NotificationType.SUCCESS);
    }

    /**
     * Show warning notification
     */
    public void showWarning(String message) {
        show(message, NotificationType.WARNING);
    }

    /**
     * Show error notification
     */
    public void showError(String message) {
        show(message, NotificationType.ERROR, Duration.seconds(10)); // Longer for errors
    }

    /**
     * Show persistent notification (no auto-dismiss)
     */
    public void showPersistent(String message, NotificationType type) {
        show(message, type, null);
    }

    /**
     * Clear all notifications
     */
    public void clearAll() {
        getChildren().forEach(node -> {
            FadeTransition fadeOut = new FadeTransition(FADE_DURATION, node);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> getChildren().remove(node));
            fadeOut.play();
        });
        showing.set(false);
    }

    /**
     * Check if any notifications are showing
     */
    public BooleanProperty showingProperty() {
        return showing;
    }

    public boolean isShowing() {
        return showing.get();
    }

    /**
     * Create a notification box
     */
    private HBox createNotification(String message, NotificationType type, Duration autoDismiss) {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().addAll("notification", type.getStyleClass());
        box.setPadding(new Insets(12, 16, 12, 16));
        box.setMaxWidth(600);

        // Icon
        Label icon = new Label(getIconText(type));
        icon.setStyle("-fx-font-size: 18px;");

        // Message
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        HBox.setHgrow(messageLabel, Priority.ALWAYS);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.SOMETIMES);

        // Close button
        Button closeBtn = new Button("\u2715"); // X symbol
        closeBtn.getStyleClass().add("notification-close");
        closeBtn.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-border-color: transparent; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0 4;"
        );
        closeBtn.setOnAction(e -> dismiss(box));

        box.getChildren().addAll(icon, messageLabel, spacer, closeBtn);

        // Auto-dismiss
        if (autoDismiss != null) {
            SequentialTransition autoClose = new SequentialTransition(
                    new PauseTransition(autoDismiss),
                    createFadeOut(box)
            );
            autoClose.play();
        }

        return box;
    }

    /**
     * Dismiss a specific notification
     */
    private void dismiss(HBox notification) {
        FadeTransition fadeOut = createFadeOut(notification);
        fadeOut.play();
    }

    /**
     * Create fade out animation
     */
    private FadeTransition createFadeOut(HBox notification) {
        FadeTransition fadeOut = new FadeTransition(FADE_DURATION, notification);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            getChildren().remove(notification);
            if (getChildren().isEmpty()) {
                showing.set(false);
            }
        });
        return fadeOut;
    }

    /**
     * Get icon text for type
     */
    private String getIconText(NotificationType type) {
        return switch (type) {
            case INFO -> "\u2139"; // Info symbol
            case SUCCESS -> "\u2713"; // Check mark
            case WARNING -> "\u26A0"; // Warning symbol
            case ERROR -> "\u2717"; // X mark
        };
    }
}
