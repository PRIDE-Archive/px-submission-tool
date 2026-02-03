package uk.ac.ebi.pride.pxsubmit.view.dialog;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Helper class for creating consistent dialogs throughout the application.
 *
 * Features:
 * - Error dialogs with expandable stack traces
 * - Confirmation dialogs with custom buttons
 * - Input dialogs for text entry
 * - Progress dialogs for long operations
 * - Consistent styling
 */
public class DialogHelper {

    private static Window ownerWindow;

    /**
     * Set the default owner window for dialogs
     */
    public static void setOwnerWindow(Window window) {
        ownerWindow = window;
    }

    // ==================== Error Dialogs ====================

    /**
     * Show error dialog
     */
    public static void showError(String title, String message) {
        showError(title, message, null);
    }

    /**
     * Show error dialog with exception details
     */
    public static void showError(String title, String message, Throwable exception) {
        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            configureDialog(alert, title, message);

            if (exception != null) {
                // Create expandable stack trace
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                exception.printStackTrace(pw);
                String stackTrace = sw.toString();

                Label label = new Label("Exception details:");
                TextArea textArea = new TextArea(stackTrace);
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setMaxWidth(Double.MAX_VALUE);
                textArea.setMaxHeight(Double.MAX_VALUE);
                GridPane.setVgrow(textArea, Priority.ALWAYS);
                GridPane.setHgrow(textArea, Priority.ALWAYS);

                GridPane expandableContent = new GridPane();
                expandableContent.setMaxWidth(Double.MAX_VALUE);
                expandableContent.add(label, 0, 0);
                expandableContent.add(textArea, 0, 1);

                alert.getDialogPane().setExpandableContent(expandableContent);
            }

            alert.showAndWait();
        });
    }

    /**
     * Show error with retry option
     */
    public static CompletableFuture<Boolean> showRetryError(String title, String message) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            configureDialog(alert, title, message);

            ButtonType retryButton = new ButtonType("Retry", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(retryButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();
            future.complete(result.isPresent() && result.get() == retryButton);
        });

        return future;
    }

    // ==================== Warning Dialogs ====================

    /**
     * Show warning dialog
     */
    public static void showWarning(String title, String message) {
        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            configureDialog(alert, title, message);
            alert.showAndWait();
        });
    }

    // ==================== Info Dialogs ====================

    /**
     * Show information dialog
     */
    public static void showInfo(String title, String message) {
        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            configureDialog(alert, title, message);
            alert.showAndWait();
        });
    }

    // ==================== Confirmation Dialogs ====================

    /**
     * Show confirmation dialog
     */
    public static CompletableFuture<Boolean> showConfirmation(String title, String message) {
        return showConfirmation(title, message, "Yes", "No");
    }

    /**
     * Show confirmation with custom buttons
     */
    public static CompletableFuture<Boolean> showConfirmation(String title, String message,
                                                               String confirmText, String cancelText) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            configureDialog(alert, title, message);

            ButtonType confirmButton = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType(cancelText, ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(confirmButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();
            future.complete(result.isPresent() && result.get() == confirmButton);
        });

        return future;
    }

    /**
     * Show save/discard/cancel dialog
     */
    public static CompletableFuture<SaveChoice> showSaveConfirmation(String title, String message) {
        CompletableFuture<SaveChoice> future = new CompletableFuture<>();

        runOnFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            configureDialog(alert, title, message);

            ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.YES);
            ButtonType discardButton = new ButtonType("Don't Save", ButtonBar.ButtonData.NO);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() == cancelButton) {
                future.complete(SaveChoice.CANCEL);
            } else if (result.get() == saveButton) {
                future.complete(SaveChoice.SAVE);
            } else {
                future.complete(SaveChoice.DISCARD);
            }
        });

        return future;
    }

    public enum SaveChoice {
        SAVE, DISCARD, CANCEL
    }

    // ==================== Input Dialogs ====================

    /**
     * Show text input dialog
     */
    public static CompletableFuture<Optional<String>> showTextInput(String title, String message,
                                                                     String defaultValue) {
        CompletableFuture<Optional<String>> future = new CompletableFuture<>();

        runOnFxThread(() -> {
            TextInputDialog dialog = new TextInputDialog(defaultValue);
            dialog.setTitle(title);
            dialog.setHeaderText(null);
            dialog.setContentText(message);

            if (ownerWindow != null) {
                dialog.initOwner(ownerWindow);
            }

            future.complete(dialog.showAndWait());
        });

        return future;
    }

    /**
     * Show choice dialog
     */
    @SafeVarargs
    public static <T> CompletableFuture<Optional<T>> showChoice(String title, String message,
                                                                 T defaultChoice, T... choices) {
        CompletableFuture<Optional<T>> future = new CompletableFuture<>();

        runOnFxThread(() -> {
            ChoiceDialog<T> dialog = new ChoiceDialog<>(defaultChoice, choices);
            dialog.setTitle(title);
            dialog.setHeaderText(null);
            dialog.setContentText(message);

            if (ownerWindow != null) {
                dialog.initOwner(ownerWindow);
            }

            future.complete(dialog.showAndWait());
        });

        return future;
    }

    // ==================== Progress Dialogs ====================

    /**
     * Create a progress dialog
     */
    public static ProgressDialog createProgressDialog(String title, String message) {
        return new ProgressDialog(title, message);
    }

    /**
     * Progress dialog for long operations
     */
    public static class ProgressDialog {
        private final Dialog<Void> dialog;
        private final ProgressBar progressBar;
        private final Label statusLabel;
        private final Button cancelButton;
        private Runnable onCancel;

        public ProgressDialog(String title, String message) {
            dialog = new Dialog<>();
            dialog.setTitle(title);
            dialog.setHeaderText(message);

            if (ownerWindow != null) {
                dialog.initOwner(ownerWindow);
            }

            VBox content = new VBox(15);
            content.setPadding(new Insets(20));
            content.setMinWidth(400);

            progressBar = new ProgressBar(-1); // Indeterminate
            progressBar.setMaxWidth(Double.MAX_VALUE);

            statusLabel = new Label("Please wait...");
            statusLabel.setStyle("-fx-text-fill: #666;");

            cancelButton = new Button("Cancel");
            cancelButton.setOnAction(e -> {
                if (onCancel != null) {
                    onCancel.run();
                }
                close();
            });

            content.getChildren().addAll(progressBar, statusLabel, cancelButton);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);
        }

        public void show() {
            runOnFxThread(() -> dialog.show());
        }

        public void close() {
            runOnFxThread(() -> dialog.close());
        }

        public void setProgress(double progress) {
            runOnFxThread(() -> progressBar.setProgress(progress));
        }

        public void setStatus(String status) {
            runOnFxThread(() -> statusLabel.setText(status));
        }

        public void setOnCancel(Runnable handler) {
            this.onCancel = handler;
        }

        public void setCancelable(boolean cancelable) {
            runOnFxThread(() -> cancelButton.setVisible(cancelable));
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Configure common dialog properties
     */
    private static void configureDialog(Alert alert, String title, String message) {
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        if (ownerWindow != null) {
            alert.initOwner(ownerWindow);
        }

        // Apply consistent styling
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setMinWidth(400);
    }

    /**
     * Run on JavaFX thread
     */
    private static void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}
