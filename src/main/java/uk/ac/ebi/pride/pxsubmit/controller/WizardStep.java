package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.beans.property.BooleanProperty;
import javafx.scene.Parent;

/**
 * Interface for wizard steps in the submission workflow.
 * Replaces the complex Form + NavigationPanelDescriptor pattern with a simple interface.
 *
 * Each step provides:
 * - Title and description for the header
 * - Content (the view to display)
 * - Validation state (observable for button binding)
 * - Lifecycle callbacks for entering/leaving
 *
 * Usage:
 * <pre>
 * public class FileSelectionStep implements WizardStep {
 *     @Override public String getTitle() { return "Select Files"; }
 *     @Override public Parent getContent() { return fxmlLoader.load(); }
 *     @Override public BooleanProperty validProperty() { return hasFilesSelected; }
 * }
 * </pre>
 */
public interface WizardStep {

    /**
     * Unique identifier for this step
     */
    String getId();

    /**
     * Title displayed in the wizard header
     */
    String getTitle();

    /**
     * Description displayed below the title
     */
    String getDescription();

    /**
     * The content view for this step (typically loaded from FXML)
     */
    Parent getContent();

    /**
     * Observable property indicating whether this step is valid.
     * Used to enable/disable the Next button.
     */
    BooleanProperty validProperty();

    /**
     * Called when the step becomes visible (after transition completes).
     * Use for initializing data, starting animations, etc.
     */
    default void onEntering() {
        // Default: no-op
    }

    /**
     * Called before leaving the step (before transition starts).
     * Use for cleanup, saving state, etc.
     */
    default void onLeaving() {
        // Default: no-op
    }

    /**
     * Validate the step before allowing navigation to next step.
     * This is called when user clicks Next.
     *
     * @return true if validation passes, false to block navigation
     */
    default boolean validate() {
        return validProperty().get();
    }

    /**
     * Check if this step can be skipped (e.g., optional steps).
     * Skipped steps are not shown in navigation.
     */
    default boolean canSkip() {
        return false;
    }

    /**
     * Check if the Back button should be visible on this step.
     * First step typically returns false.
     */
    default boolean showBackButton() {
        return true;
    }

    /**
     * Get the text for the Next button.
     * Override for final step to show "Submit" instead of "Next".
     */
    default String getNextButtonText() {
        return "Next";
    }

    /**
     * Check if this is the final step (submission step).
     */
    default boolean isFinalStep() {
        return false;
    }
}
