package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;

/**
 * Step for entering project metadata.
 * Collects: title, description, keywords, sample/data processing protocols.
 */
public class ProjectMetadataStep extends AbstractWizardStep {

    // Form fields
    private TextField titleField;
    private TextArea descriptionArea;
    private TextField keywordsField;
    private TextArea sampleProtocolArea;
    private TextArea dataProtocolArea;

    // Character counters
    private Label titleCounter;
    private Label descriptionCounter;

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
        descriptionCounter = new Label("0 / " + MAX_DESCRIPTION_LENGTH);
        descriptionCounter.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        descSection.getChildren().addAll(descriptionArea, descriptionCounter);

        // Keywords
        VBox keywordsSection = createSection("Keywords *",
            "Comma-separated keywords to help others find your dataset");
        keywordsField = new TextField();
        keywordsField.setPromptText("e.g., liver cancer, proteomics, mass spectrometry, biomarker");
        keywordsSection.getChildren().add(keywordsField);

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
        dataSection.getChildren().add(dataProtocolArea);

        // Required fields note
        Label requiredNote = new Label("* Required fields");
        requiredNote.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");

        form.getChildren().addAll(
            titleSection,
            descSection,
            keywordsSection,
            sampleSection,
            dataSection,
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
        keywordsField.textProperty().bindBidirectional(model.keywordsProperty());
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
        });

        // Validation binding
        valid.bind(Bindings.createBooleanBinding(() -> {
            String title = titleField.getText();
            String desc = descriptionArea.getText();
            String keywords = keywordsField.getText();
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
        keywordsField.textProperty(),
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

        if (isNullOrEmpty(keywordsField.getText())) {
            showError("Please enter at least one keyword");
            keywordsField.requestFocus();
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

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
