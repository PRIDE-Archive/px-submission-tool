package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.animation.FadeTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.util.DebugMode;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the wizard navigation.
 * Manages step transitions, button states, and header updates.
 *
 * This replaces the complex Navigator + NavigationModel + NavigationController pattern
 * with a simple, direct implementation using JavaFX.
 *
 * Features:
 * - Automatic button state binding based on current step
 * - Smooth transitions between steps
 * - Step lifecycle management (onEntering, onLeaving, validate)
 * - Training mode indication in header
 */
public class WizardController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(WizardController.class);

    // FXML injected components
    @FXML private VBox headerPane;
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label trainingModeLabel;
    @FXML private Label stepIndicatorLabel;
    @FXML private ProgressBar stepProgressBar;

    @FXML private StackPane contentPane;

    @FXML private HBox buttonBar;
    @FXML private Button helpButton;
    @FXML private Button cancelButton;
    @FXML private Button backButton;
    @FXML private Button nextButton;

    // Model
    private SubmissionModel model;

    // Steps
    private final ObservableList<WizardStep> steps = FXCollections.observableArrayList();
    private final IntegerProperty currentStepIndex = new SimpleIntegerProperty(-1);

    // Current step property (computed)
    private final ObjectProperty<WizardStep> currentStep = new SimpleObjectProperty<>();

    // State
    private final BooleanProperty canGoBack = new SimpleBooleanProperty(false);
    private final BooleanProperty canGoNext = new SimpleBooleanProperty(false);
    private final BooleanProperty isFinished = new SimpleBooleanProperty(false);

    // Animation duration
    private static final Duration TRANSITION_DURATION = Duration.millis(200);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bind current step to index
        currentStep.bind(Bindings.createObjectBinding(() -> {
            int index = currentStepIndex.get();
            if (index >= 0 && index < steps.size()) {
                return steps.get(index);
            }
            return null;
        }, currentStepIndex, steps));

        // Bind header labels to current step
        titleLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            WizardStep step = currentStep.get();
            return step != null ? step.getTitle() : "";
        }, currentStep));

        descriptionLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            WizardStep step = currentStep.get();
            return step != null ? step.getDescription() : "";
        }, currentStep));

        // Bind step progress indicator
        if (stepIndicatorLabel != null) {
            stepIndicatorLabel.textProperty().bind(Bindings.createStringBinding(() -> {
                int current = currentStepIndex.get() + 1;
                int total = steps.size();
                return String.format("Step %d of %d", current, total);
            }, currentStepIndex, steps));
        }

        if (stepProgressBar != null) {
            stepProgressBar.progressProperty().bind(Bindings.createDoubleBinding(() -> {
                if (steps.isEmpty()) return 0.0;
                return (double) (currentStepIndex.get() + 1) / steps.size();
            }, currentStepIndex, steps));
        }

        // Bind button states
        canGoBack.bind(Bindings.createBooleanBinding(() -> {
            WizardStep step = currentStep.get();
            return step != null && step.showBackButton() && currentStepIndex.get() > 0;
        }, currentStep, currentStepIndex));

        canGoNext.bind(Bindings.createBooleanBinding(() -> {
            WizardStep step = currentStep.get();
            return step != null && step.validProperty().get();
        }, currentStep));

        backButton.disableProperty().bind(canGoBack.not());
        backButton.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
            WizardStep step = currentStep.get();
            return step != null && step.showBackButton();
        }, currentStep));

        // Next button text changes on final step
        nextButton.textProperty().bind(Bindings.createStringBinding(() -> {
            WizardStep step = currentStep.get();
            return step != null ? step.getNextButtonText() : "Next";
        }, currentStep));

        // Set button actions
        cancelButton.setOnAction(e -> handleCancel());
        backButton.setOnAction(e -> handleBack());
        nextButton.setOnAction(e -> handleNext());
        helpButton.setOnAction(e -> handleHelp());

        // Listen to step changes for dynamic button binding
        currentStep.addListener((obs, oldStep, newStep) -> {
            if (newStep != null) {
                // Rebind next button disable to new step's validProperty
                nextButton.disableProperty().unbind();
                nextButton.disableProperty().bind(newStep.validProperty().not());

                // Rebind next button text (depends on step state, e.g. "Submit" -> "Finish")
                nextButton.textProperty().unbind();
                nextButton.textProperty().bind(Bindings.createStringBinding(
                    newStep::getNextButtonText,
                    newStep.validProperty()
                ));
            }
        });
    }

    /**
     * Set the submission model
     */
    public void setModel(SubmissionModel model) {
        this.model = model;

        // Bind training mode indicator
        if (trainingModeLabel != null) {
            trainingModeLabel.visibleProperty().bind(model.trainingModeProperty());
            trainingModeLabel.managedProperty().bind(model.trainingModeProperty());
        }
    }

    /**
     * Register a wizard step
     */
    public void addStep(WizardStep step) {
        steps.add(step);
        // Set wizard controller reference if step extends AbstractWizardStep
        if (step instanceof AbstractWizardStep) {
            ((AbstractWizardStep) step).setWizardController(this);
        }
        logger.debug("Registered step: {} ({})", step.getId(), step.getTitle());
    }

    /**
     * Start the wizard at the first step
     */
    public void start() {
        if (steps.isEmpty()) {
            logger.error("No steps registered!");
            return;
        }
        goToStep(0);
    }

    /**
     * Navigate to a specific step by index
     */
    public void goToStep(int index) {
        if (index < 0 || index >= steps.size()) {
            logger.warn("Invalid step index: {}", index);
            return;
        }

        // Skip steps that should be skipped
        while (index < steps.size() && steps.get(index).canSkip()) {
            index++;
        }

        if (index >= steps.size()) {
            logger.warn("All remaining steps are skippable");
            return;
        }

        WizardStep oldStep = currentStep.get();
        WizardStep newStep = steps.get(index);

        // Call lifecycle method on old step
        if (oldStep != null) {
            oldStep.onLeaving();
        }

        // Update index
        int oldIndex = currentStepIndex.get();
        currentStepIndex.set(index);

        // Transition content
        transitionContent(newStep.getContent(), oldIndex < index);

        // Call lifecycle method on new step
        newStep.onEntering();

        logger.info("Navigated to step {} of {}: {}", index + 1, steps.size(), newStep.getTitle());
    }

    /**
     * Navigate to a step by ID
     */
    public void goToStep(String stepId) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getId().equals(stepId)) {
                goToStep(i);
                return;
            }
        }
        logger.warn("Step not found: {}", stepId);
    }

    /**
     * Handle Cancel button
     */
    private void handleCancel() {
        logger.info("Cancel requested");
        // This will be handled by the main application
        if (onCancelHandler != null) {
            onCancelHandler.run();
        }
    }

    /**
     * Handle Back button
     */
    private void handleBack() {
        int prevIndex = currentStepIndex.get() - 1;

        // Skip skippable steps going backwards
        while (prevIndex >= 0 && steps.get(prevIndex).canSkip()) {
            prevIndex--;
        }

        if (prevIndex >= 0) {
            goToStep(prevIndex);
        }
    }

    /**
     * Handle Next button
     */
    private void handleNext() {
        WizardStep step = currentStep.get();
        if (step == null) return;

        DebugMode.log("WIZARD", "Next clicked on step: " + step.getTitle());

        // Validate current step
        if (!step.validate()) {
            logger.info("Step validation failed: {}", step.getTitle());
            DebugMode.log("WIZARD", "Validation FAILED for step: " + step.getTitle());
            return;
        }

        DebugMode.log("WIZARD", "Validation PASSED for step: " + step.getTitle());

        // Check if final step
        if (step.isFinalStep()) {
            handleFinish();
            return;
        }

        // Go to next step
        int nextIndex = currentStepIndex.get() + 1;
        if (nextIndex < steps.size()) {
            DebugMode.log("WIZARD", "Navigating to step index: " + nextIndex);
            goToStep(nextIndex);
        }
    }

    /**
     * Handle Help button
     */
    private void handleHelp() {
        logger.info("Help requested");
        if (onHelpHandler != null) {
            onHelpHandler.run();
        }
    }

    /**
     * Handle finish (submission complete)
     */
    private void handleFinish() {
        logger.info("Wizard finished");
        isFinished.set(true);
        if (onFinishHandler != null) {
            onFinishHandler.run();
        }
    }

    /**
     * Transition between step contents with fade animation
     */
    private void transitionContent(Parent newContent, boolean forward) {
        Parent oldContent = contentPane.getChildren().isEmpty() ? null :
                           (Parent) contentPane.getChildren().get(0);

        if (oldContent == null) {
            // First step - just add content
            contentPane.getChildren().add(newContent);
            return;
        }

        // Fade out old content, then replace with new
        FadeTransition fadeOut = new FadeTransition(TRANSITION_DURATION, oldContent);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            contentPane.getChildren().clear();
            newContent.setOpacity(0);
            contentPane.getChildren().add(newContent);

            FadeTransition fadeIn = new FadeTransition(TRANSITION_DURATION, newContent);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }

    // ==================== Event Handlers ====================

    private Runnable onCancelHandler;
    private Runnable onHelpHandler;
    private Runnable onFinishHandler;

    public void setOnCancel(Runnable handler) {
        this.onCancelHandler = handler;
    }

    public void setOnHelp(Runnable handler) {
        this.onHelpHandler = handler;
    }

    public void setOnFinish(Runnable handler) {
        this.onFinishHandler = handler;
    }

    // ==================== Property Accessors ====================

    public ReadOnlyIntegerProperty currentStepIndexProperty() {
        return currentStepIndex;
    }

    public int getCurrentStepIndex() {
        return currentStepIndex.get();
    }

    public ReadOnlyObjectProperty<WizardStep> currentStepProperty() {
        return currentStep;
    }

    public WizardStep getCurrentStep() {
        return currentStep.get();
    }

    public ObservableList<WizardStep> getSteps() {
        return FXCollections.unmodifiableObservableList(steps);
    }

    public int getStepCount() {
        return steps.size();
    }

    public ReadOnlyBooleanProperty finishedProperty() {
        return isFinished;
    }

    public boolean isFinished() {
        return isFinished.get();
    }

    /**
     * Get step by ID
     */
    public Optional<WizardStep> getStep(String stepId) {
        return steps.stream()
                .filter(s -> s.getId().equals(stepId))
                .findFirst();
    }

    /**
     * Enable/disable cancel button
     */
    public void setCancelEnabled(boolean enabled) {
        cancelButton.setDisable(!enabled);
    }

    /**
     * Enable/disable navigation (e.g., during long operations).
     * Must unbind before setting because button disable properties are bound to step validity.
     */
    public void setNavigationEnabled(boolean enabled) {
        if (!enabled) {
            // Unbind before disabling so we can override the bound values
            nextButton.disableProperty().unbind();
            nextButton.setDisable(true);
            backButton.disableProperty().unbind();
            backButton.setDisable(true);
            cancelButton.setDisable(true);
        } else {
            // Re-bind to current step's valid property
            WizardStep step = currentStep.get();
            if (step != null) {
                nextButton.disableProperty().bind(step.validProperty().not());
            } else {
                nextButton.setDisable(false);
            }
            backButton.disableProperty().bind(canGoBack.not());
            cancelButton.setDisable(false);
        }
    }

    /**
     * Navigate to the next step (for programmatic navigation)
     */
    public void goToNextStep() {
        int nextIndex = currentStepIndex.get() + 1;
        if (nextIndex < steps.size()) {
            goToStep(nextIndex);
        }
    }
}
