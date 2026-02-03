package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.util.DebugMode;

import java.io.IOException;

/**
 * Abstract base class for wizard steps.
 * Provides common functionality and FXML loading support.
 *
 * Usage:
 * <pre>
 * public class FileSelectionStep extends AbstractWizardStep {
 *     public FileSelectionStep(SubmissionModel model) {
 *         super("file-selection", "Select Files",
 *               "Choose the files to include in your submission", model);
 *     }
 *
 *     @Override
 *     protected String getFxmlPath() {
 *         return "/fxml/steps/FileSelectionStep.fxml";
 *     }
 *
 *     @Override
 *     protected void initializeStep() {
 *         // Bind UI components to model
 *         valid.bind(model.getFiles().isNotEmpty());
 *     }
 * }
 * </pre>
 */
public abstract class AbstractWizardStep implements WizardStep {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final String id;
    private final String title;
    private final String description;

    protected final SubmissionModel model;
    protected final BooleanProperty valid = new SimpleBooleanProperty(false);

    private Parent content;
    private boolean initialized = false;

    protected AbstractWizardStep(String id, String title, String description, SubmissionModel model) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.model = model;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Parent getContent() {
        if (content == null) {
            loadContent();
        }
        return content;
    }

    @Override
    public BooleanProperty validProperty() {
        return valid;
    }

    @Override
    public void onEntering() {
        logger.debug("Entering step: {}", title);
        DebugMode.log("WIZARD", "Entering step: " + title + " (valid=" + valid.get() + ")");
        if (!initialized) {
            DebugMode.log("WIZARD", "Initializing step: " + title);
            initializeStep();
            initialized = true;
        }
        onStepEntering();
    }

    @Override
    public void onLeaving() {
        logger.debug("Leaving step: {}", title);
        DebugMode.log("WIZARD", "Leaving step: " + title + " (valid=" + valid.get() + ")");
        onStepLeaving();
    }

    /**
     * Load FXML content
     */
    private void loadContent() {
        String fxmlPath = getFxmlPath();
        if (fxmlPath != null && !fxmlPath.isEmpty()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                loader.setController(this);
                content = loader.load();
                logger.debug("Loaded FXML: {}", fxmlPath);
            } catch (IOException e) {
                logger.error("Failed to load FXML: {}", fxmlPath, e);
                throw new RuntimeException("Failed to load step content: " + fxmlPath, e);
            }
        } else {
            // Subclass should override createContent()
            content = createContent();
        }
    }

    /**
     * Get the FXML file path for this step.
     * Override to specify FXML file, or return null to use createContent().
     *
     * @return path to FXML file (e.g., "/fxml/steps/LoginStep.fxml")
     */
    protected String getFxmlPath() {
        return null;
    }

    /**
     * Create content programmatically.
     * Override if not using FXML.
     *
     * @return the content Parent node
     */
    protected Parent createContent() {
        throw new UnsupportedOperationException(
            "Either override getFxmlPath() or createContent() to provide step content");
    }

    /**
     * Initialize the step.
     * Called once when the step is first entered.
     * Override to set up bindings, listeners, etc.
     */
    protected void initializeStep() {
        // Default: no-op
    }

    /**
     * Called each time the step is entered.
     * Override for step-specific entering logic.
     */
    protected void onStepEntering() {
        // Default: no-op
    }

    /**
     * Called each time the step is left.
     * Override for step-specific leaving logic.
     */
    protected void onStepLeaving() {
        // Default: no-op
    }

    /**
     * Get the submission model
     */
    protected SubmissionModel getModel() {
        return model;
    }

    /**
     * Set valid state
     */
    protected void setValid(boolean isValid) {
        valid.set(isValid);
    }

    /**
     * Check if valid
     */
    protected boolean isValid() {
        return valid.get();
    }
}
