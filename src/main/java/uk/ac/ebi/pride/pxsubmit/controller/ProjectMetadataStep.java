package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.view.component.ChipInput;
import uk.ac.ebi.pride.pxsubmit.view.component.ValidationFeedback;

/**
 * Step for entering project metadata.
 * Collects: title, description, keywords, sample/data processing protocols.
 */
public class ProjectMetadataStep extends AbstractWizardStep {

    // Form fields
    private TextField titleField;
    private TextArea descriptionArea;
    private ChipInput keywordsInput;
    private TextArea sampleProtocolArea;
    private TextArea dataProtocolArea;

    // Character counters
    private Label titleCounter;
    private Label descriptionCounter;

    // Validation feedback
    private ValidationFeedback validationFeedback;

    // Validation
    private static final int MIN_TITLE_LENGTH = 5;
    private static final int MAX_TITLE_LENGTH = 500;
    private static final int MIN_DESCRIPTION_LENGTH = 20;
    private static final int MAX_DESCRIPTION_LENGTH = 5000;

    public ProjectMetadataStep(SubmissionModel model) {
        super("project-metadata",
              "Project Information",
              "Enter the details about your submission",
              model);
    }

    @Override
    protected Parent createContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox form = new VBox(20);
        form.setPadding(new Insets(20));

        // Project Title
        VBox titleSection = createSection("Project Title *",
            "A short descriptive title for your project (5-500 characters)");
        titleField = new TextField();
        titleField.setPromptText("e.g., Proteome analysis of human liver cancer cells");
        Tooltip titleTooltip = new Tooltip(
            "Enter a descriptive title for your project.\n" +
            "This will be displayed in PRIDE Archive.\n" +
            "Minimum 5 characters, maximum 500.");
        titleTooltip.setShowDelay(Duration.millis(300));
        titleField.setTooltip(titleTooltip);
        titleCounter = new Label("0 / " + MAX_TITLE_LENGTH);
        titleCounter.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        HBox titleBox = new HBox(10);
        HBox.setHgrow(titleField, Priority.ALWAYS);
        titleBox.getChildren().addAll(titleField, titleCounter);
        titleSection.getChildren().add(titleBox);

        // Project Description
        VBox descSection = createSection("Project Description *",
            "Detailed description of your project (20-5000 characters)");
        descriptionArea = new TextArea();
        descriptionArea.setPromptText(
            "Describe your project, including:\n" +
            "• Scientific background and aims\n" +
            "• Experimental approach\n" +
            "• Key findings (if applicable)");
        descriptionArea.setPrefRowCount(6);
        descriptionArea.setWrapText(true);
        Tooltip descTooltip = new Tooltip(
            "Provide a detailed description of your project.\n" +
            "Include scientific background, aims, and key findings.\n" +
            "Minimum 20 characters, maximum 5000.");
        descTooltip.setShowDelay(Duration.millis(300));
        descriptionArea.setTooltip(descTooltip);
        descriptionCounter = new Label("0 / " + MAX_DESCRIPTION_LENGTH);
        descriptionCounter.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        descSection.getChildren().addAll(descriptionArea, descriptionCounter);

        // Keywords
        VBox keywordsSection = createSection("Keywords *",
            "Type a keyword and press Enter to add it. Click \u2715 to remove.");
        keywordsInput = new ChipInput();
        keywordsInput.setPromptText("Type keyword and press Enter...");
        Tooltip keywordsTooltip = new Tooltip(
            "Add keywords that describe your dataset.\n" +
            "Type a word and press Enter to add it as a tag.\n" +
            "Click X on a tag to remove it.");
        keywordsTooltip.setShowDelay(Duration.millis(300));
        Tooltip.install(keywordsInput, keywordsTooltip);
        Label keywordsHint = new Label("Examples: liver cancer, proteomics, mass spectrometry, biomarker");
        keywordsHint.setStyle("-fx-text-fill: #999; -fx-font-size: 11px; -fx-font-style: italic;");
        keywordsSection.getChildren().addAll(keywordsInput, keywordsHint);

        // Sample Processing Protocol
        VBox sampleSection = createSection("Sample Processing Protocol *",
            "Describe how samples were prepared for analysis");
        sampleProtocolArea = new TextArea();
        sampleProtocolArea.setPromptText(
            "Describe sample processing steps:\n" +
            "• Sample collection and storage\n" +
            "• Protein extraction method\n" +
            "• Digestion protocol\n" +
            "• Fractionation (if applicable)");
        sampleProtocolArea.setPrefRowCount(4);
        sampleProtocolArea.setWrapText(true);
        Tooltip sampleTooltip = new Tooltip(
            "Describe your sample preparation protocol.\n" +
            "Include extraction methods, digestion, and fractionation.");
        sampleTooltip.setShowDelay(Duration.millis(300));
        sampleProtocolArea.setTooltip(sampleTooltip);
        sampleSection.getChildren().add(sampleProtocolArea);

        // Data Processing Protocol
        VBox dataSection = createSection("Data Processing Protocol *",
            "Describe how data was analyzed");
        dataProtocolArea = new TextArea();
        dataProtocolArea.setPromptText(
            "Describe data processing:\n" +
            "• Search engine and version\n" +
            "• Database used\n" +
            "• Search parameters\n" +
            "• Post-processing/validation");
        dataProtocolArea.setPrefRowCount(4);
        dataProtocolArea.setWrapText(true);
        Tooltip dataTooltip = new Tooltip(
            "Describe your data analysis workflow.\n" +
            "Include search engine, database, and parameters used.");
        dataTooltip.setShowDelay(Duration.millis(300));
        dataProtocolArea.setTooltip(dataTooltip);
        dataSection.getChildren().add(dataProtocolArea);

        // Validation feedback
        validationFeedback = new ValidationFeedback();

        // Required fields note
        Label requiredNote = new Label("* Required fields");
        requiredNote.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");

        form.getChildren().addAll(
            titleSection,
            descSection,
            keywordsSection,
            sampleSection,
            dataSection,
            validationFeedback,
            requiredNote
        );

        scrollPane.setContent(form);
        return scrollPane;
    }

    private VBox createSection(String title, String description) {
        VBox section = new VBox(5);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        section.getChildren().addAll(titleLabel, descLabel);
        return section;
    }

    @Override
    protected void initializeStep() {
        // Bidirectional bindings to model
        titleField.textProperty().bindBidirectional(model.projectTitleProperty());
        descriptionArea.textProperty().bindBidirectional(model.projectDescriptionProperty());
        keywordsInput.textProperty().bindBidirectional(model.keywordsProperty());
        sampleProtocolArea.textProperty().bindBidirectional(model.sampleProcessingProtocolProperty());
        dataProtocolArea.textProperty().bindBidirectional(model.dataProcessingProtocolProperty());

        // Character counters
        titleField.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal != null ? newVal.length() : 0;
            titleCounter.setText(len + " / " + MAX_TITLE_LENGTH);
            if (len > MAX_TITLE_LENGTH) {
                titleCounter.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px;");
            } else if (len >= MIN_TITLE_LENGTH) {
                titleCounter.setStyle("-fx-text-fill: #28a745; -fx-font-size: 11px;");
            } else {
                titleCounter.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
            }
            updateValidationFeedback();
        });

        descriptionArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal != null ? newVal.length() : 0;
            descriptionCounter.setText(len + " / " + MAX_DESCRIPTION_LENGTH);
            if (len > MAX_DESCRIPTION_LENGTH) {
                descriptionCounter.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px;");
            } else if (len >= MIN_DESCRIPTION_LENGTH) {
                descriptionCounter.setStyle("-fx-text-fill: #28a745; -fx-font-size: 11px;");
            } else {
                descriptionCounter.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
            }
            updateValidationFeedback();
        });

        // Update validation feedback when other fields change
        keywordsInput.textProperty().addListener((obs, oldVal, newVal) -> updateValidationFeedback());
        sampleProtocolArea.textProperty().addListener((obs, oldVal, newVal) -> updateValidationFeedback());
        dataProtocolArea.textProperty().addListener((obs, oldVal, newVal) -> updateValidationFeedback());

        // Validation binding
        valid.bind(Bindings.createBooleanBinding(() -> {
            String title = titleField.getText();
            String desc = descriptionArea.getText();
            String keywords = keywordsInput.getText();
            String sampleProtocol = sampleProtocolArea.getText();
            String dataProtocol = dataProtocolArea.getText();

            return isValidTitle(title) &&
                   isValidDescription(desc) &&
                   isNotEmpty(keywords) &&
                   isNotEmpty(sampleProtocol) &&
                   isNotEmpty(dataProtocol);
        },
        titleField.textProperty(),
        descriptionArea.textProperty(),
        keywordsInput.textProperty(),
        sampleProtocolArea.textProperty(),
        dataProtocolArea.textProperty()));
    }

    @Override
    public boolean validate() {
        // Check each field and show specific error
        String title = titleField.getText();
        if (title == null || title.trim().length() < MIN_TITLE_LENGTH) {
            showError("Project title must be at least " + MIN_TITLE_LENGTH + " characters");
            titleField.requestFocus();
            return false;
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            showError("Project title cannot exceed " + MAX_TITLE_LENGTH + " characters");
            titleField.requestFocus();
            return false;
        }

        String desc = descriptionArea.getText();
        if (desc == null || desc.trim().length() < MIN_DESCRIPTION_LENGTH) {
            showError("Project description must be at least " + MIN_DESCRIPTION_LENGTH + " characters");
            descriptionArea.requestFocus();
            return false;
        }
        if (desc.length() > MAX_DESCRIPTION_LENGTH) {
            showError("Project description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
            descriptionArea.requestFocus();
            return false;
        }

        if (keywordsInput.isEmpty()) {
            showError("Please enter at least one keyword");
            keywordsInput.requestFocus();
            return false;
        }

        if (isNullOrEmpty(sampleProtocolArea.getText())) {
            showError("Please describe the sample processing protocol");
            sampleProtocolArea.requestFocus();
            return false;
        }

        if (isNullOrEmpty(dataProtocolArea.getText())) {
            showError("Please describe the data processing protocol");
            dataProtocolArea.requestFocus();
            return false;
        }

        // Sync to model
        model.syncMetadataToSubmission();
        return true;
    }

    @Override
    protected void onStepEntering() {
        titleField.requestFocus();
    }

    private boolean isValidTitle(String text) {
        return text != null &&
               text.trim().length() >= MIN_TITLE_LENGTH &&
               text.length() <= MAX_TITLE_LENGTH;
    }

    private boolean isValidDescription(String text) {
        return text != null &&
               text.trim().length() >= MIN_DESCRIPTION_LENGTH &&
               text.length() <= MAX_DESCRIPTION_LENGTH;
    }

    private boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private boolean isNullOrEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * Update inline validation feedback based on current field values
     */
    private void updateValidationFeedback() {
        validationFeedback.clear();

        String title = titleField.getText();
        String desc = descriptionArea.getText();
        String keywords = keywordsInput.getText();
        String sampleProtocol = sampleProtocolArea.getText();
        String dataProtocol = dataProtocolArea.getText();

        int completedFields = 0;
        int totalFields = 5;

        // Title validation
        if (title == null || title.trim().length() < MIN_TITLE_LENGTH) {
            validationFeedback.addWarning("Title: Minimum " + MIN_TITLE_LENGTH + " characters required");
        } else if (title.length() > MAX_TITLE_LENGTH) {
            validationFeedback.addError("Title: Maximum " + MAX_TITLE_LENGTH + " characters exceeded");
        } else {
            completedFields++;
        }

        // Description validation
        if (desc == null || desc.trim().length() < MIN_DESCRIPTION_LENGTH) {
            validationFeedback.addWarning("Description: Minimum " + MIN_DESCRIPTION_LENGTH + " characters required");
        } else if (desc.length() > MAX_DESCRIPTION_LENGTH) {
            validationFeedback.addError("Description: Maximum " + MAX_DESCRIPTION_LENGTH + " characters exceeded");
        } else {
            completedFields++;
        }

        // Keywords validation
        if (isNullOrEmpty(keywords)) {
            validationFeedback.addWarning("Keywords: At least one keyword required");
        } else {
            completedFields++;
        }

        // Protocol validation
        if (isNullOrEmpty(sampleProtocol)) {
            validationFeedback.addWarning("Sample Processing Protocol: Required");
        } else {
            completedFields++;
        }

        if (isNullOrEmpty(dataProtocol)) {
            validationFeedback.addWarning("Data Processing Protocol: Required");
        } else {
            completedFields++;
        }

        // Show success if all complete
        if (completedFields == totalFields) {
            validationFeedback.setSuccess("All required fields complete - ready to proceed");
        } else {
            validationFeedback.addInfo(completedFields + "/" + totalFields + " required fields completed");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
