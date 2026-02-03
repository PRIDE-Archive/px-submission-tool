package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import uk.ac.ebi.pride.archive.dataprovider.utils.SubmissionTypeConstants;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;

/**
 * Step for selecting the submission type.
 * Options: Complete, Partial, Affinity
 */
public class SubmissionTypeStep extends AbstractWizardStep {

    private ToggleGroup submissionTypeGroup;
    private RadioButton completeRadio;
    private RadioButton partialRadio;
    private RadioButton affinityRadio;

    public SubmissionTypeStep(SubmissionModel model) {
        super("submission-type",
              "Select Submission Type",
              "Choose the type of submission you want to make",
              model);
    }

    @Override
    protected Parent createContent() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        // Title
        Label introLabel = new Label("Please select the type of submission that best fits your data:");
        introLabel.setStyle("-fx-font-size: 14px;");
        introLabel.setWrapText(true);

        // Radio button group
        submissionTypeGroup = new ToggleGroup();

        // Complete Submission
        VBox completeBox = createSubmissionOption(
            "Complete Submission",
            "(Recommended)",
            "Submit processed results with the corresponding raw files.\n" +
            "Supported result file formats: mzIdentML, mzTab, or PRIDE XML.\n" +
            "This is the recommended option for most proteomics experiments.",
            SubmissionTypeConstants.COMPLETE,
            true
        );

        // Partial Submission
        VBox partialBox = createSubmissionOption(
            "Partial Submission",
            "",
            "Submit search engine output files with the corresponding raw files.\n" +
            "Use this option when you don't have processed result files in\n" +
            "a supported format (mzIdentML, mzTab, or PRIDE XML).",
            SubmissionTypeConstants.PARTIAL,
            false
        );

        // Affinity Submission
        VBox affinityBox = createSubmissionOption(
            "Affinity Proteomics",
            "",
            "Submit affinity-based proteomics data such as:\n" +
            "• SomaScan (ADAT files)\n" +
            "• Olink (NPX or Parquet files)\n" +
            "Use this for non-mass spectrometry affinity experiments.",
            SubmissionTypeConstants.AFFINITY,
            false
        );

        // Links section
        HBox linksBox = new HBox(20);
        linksBox.setAlignment(Pos.CENTER);
        linksBox.setPadding(new Insets(20, 0, 0, 0));

        Hyperlink guidelinesLink = new Hyperlink("Submission Guidelines");
        guidelinesLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride/help/archive/submission"));

        Hyperlink moreInfoLink = new Hyperlink("More about ProteomeXchange");
        moreInfoLink.setOnAction(e -> openUrl("https://www.proteomexchange.org"));

        linksBox.getChildren().addAll(guidelinesLink, moreInfoLink);

        root.getChildren().addAll(introLabel, completeBox, partialBox, affinityBox, linksBox);

        return root;
    }

    private VBox createSubmissionOption(String title, String badge, String description,
                                         SubmissionTypeConstants type, boolean selected) {
        VBox optionBox = new VBox(5);
        optionBox.setPadding(new Insets(15));
        optionBox.setStyle(
            "-fx-background-color: #f8f9fa; " +
            "-fx-border-color: #dee2e6; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8;"
        );

        // Header with radio and title
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        RadioButton radio = new RadioButton();
        radio.setToggleGroup(submissionTypeGroup);
        radio.setUserData(type);
        if (selected) {
            radio.setSelected(true);
        }

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        header.getChildren().addAll(radio, titleLabel);

        if (!badge.isEmpty()) {
            Label badgeLabel = new Label(badge);
            badgeLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
            header.getChildren().add(badgeLabel);
        }

        // Description
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #666; -fx-padding: 0 0 0 30;");
        descLabel.setWrapText(true);

        optionBox.getChildren().addAll(header, descLabel);

        // Store reference
        switch (type) {
            case COMPLETE -> completeRadio = radio;
            case PARTIAL -> partialRadio = radio;
            case AFFINITY -> affinityRadio = radio;
        }

        // Click anywhere in box to select
        optionBox.setOnMouseClicked(e -> radio.setSelected(true));

        // Highlight on hover
        optionBox.setOnMouseEntered(e ->
            optionBox.setStyle(
                "-fx-background-color: #e9ecef; " +
                "-fx-border-color: #0066cc; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand;"
            ));
        optionBox.setOnMouseExited(e ->
            optionBox.setStyle(
                "-fx-background-color: #f8f9fa; " +
                "-fx-border-color: #dee2e6; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8;"
            ));

        return optionBox;
    }

    @Override
    protected void initializeStep() {
        // Always valid since one option is pre-selected
        valid.set(true);

        // Listen for selection changes
        submissionTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                SubmissionTypeConstants type = (SubmissionTypeConstants) newVal.getUserData();
                model.setSubmissionType(type);
                logger.info("Submission type selected: {}", type);
            }
            valid.set(newVal != null);
        });

        // Set initial value in model
        Toggle selected = submissionTypeGroup.getSelectedToggle();
        if (selected != null) {
            model.setSubmissionType((SubmissionTypeConstants) selected.getUserData());
        }
    }

    @Override
    protected void onStepEntering() {
        // Restore previous selection if any
        SubmissionTypeConstants currentType = model.getSubmissionType();
        if (currentType != null) {
            switch (currentType) {
                case COMPLETE -> completeRadio.setSelected(true);
                case PARTIAL -> partialRadio.setSelected(true);
                case AFFINITY -> affinityRadio.setSelected(true);
            }
        }
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            logger.error("Failed to open URL: {}", url, e);
        }
    }
}
