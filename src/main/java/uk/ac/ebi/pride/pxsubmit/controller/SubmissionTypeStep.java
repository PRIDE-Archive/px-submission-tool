package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import uk.ac.ebi.pride.archive.dataprovider.utils.SubmissionTypeConstants;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;

/**
 * Step for selecting the submission type.
 * Options: PRIDE (mass spectrometry) or Affinity Proteomics
 *
 * Note: The COMPLETE/PARTIAL distinction is handled automatically based on file types.
 * If the submission includes STANDARD file formats (mzIdentML, mzTab),
 * it will be registered as a complete ProteomeXchange submission.
 */
public class SubmissionTypeStep extends AbstractWizardStep {

    private ToggleGroup submissionTypeGroup;
    private RadioButton prideRadio;
    private RadioButton affinityRadio;

    public SubmissionTypeStep(SubmissionModel model) {
        super("submission-type",
              "Select Submission Type",
              "Choose the type of proteomics data you want to submit",
              model);
    }

    @Override
    protected Parent createContent() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        // Title
        Label introLabel = new Label("What type of proteomics data are you submitting?");
        introLabel.setStyle("-fx-font-size: 14px;");
        introLabel.setWrapText(true);

        // Radio button group
        submissionTypeGroup = new ToggleGroup();

        // PRIDE Submission (Mass Spectrometry)
        VBox prideBox = createSubmissionOption(
            "Mass Spectrometry Proteomics",
            "PRIDE",
            "Submit mass spectrometry-based proteomics data:\n\n" +
            "  \u2022 RAW files from your mass spectrometer\n" +
            "  \u2022 Analysis outputs (MaxQuant, DIA-NN, FragPipe, Spectronaut, etc.)\n" +
            "  \u2022 Protein sequence databases (FASTA)\n" +
            "  \u2022 Sample metadata (SDRF)",
            SubmissionTypeConstants.PRIDE,
            true
        );

        // Affinity Proteomics Submission
        VBox affinityBox = createSubmissionOption(
            "Affinity Proteomics",
            "Non-MS",
            "Submit affinity-based proteomics data:\n\n" +
            "  \u2022 SomaScan (ADAT files)\n" +
            "  \u2022 Olink (NPX or Parquet files)\n" +
            "  \u2022 Other antibody-based or aptamer-based assays",
            SubmissionTypeConstants.AFFINITY,
            false
        );

        // Links section
        HBox linksBox = new HBox(20);
        linksBox.setAlignment(Pos.CENTER);
        linksBox.setPadding(new Insets(20, 0, 0, 0));

        Hyperlink guidelinesLink = new Hyperlink("Submission Guidelines");
        guidelinesLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride/help/archive/submission"));

        Hyperlink prideLink = new Hyperlink("About PRIDE");
        prideLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride"));

        linksBox.getChildren().addAll(guidelinesLink, prideLink);

        root.getChildren().addAll(introLabel, prideBox, affinityBox, linksBox);

        return root;
    }

    private VBox createSubmissionOption(String title, String badge, String description,
                                         SubmissionTypeConstants type, boolean selected) {
        VBox optionBox = new VBox(8);
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

        Label badgeLabel = new Label(badge);
        badgeLabel.setStyle(
            "-fx-background-color: #0066cc; " +
            "-fx-text-fill: white; " +
            "-fx-padding: 2 8; " +
            "-fx-background-radius: 4; " +
            "-fx-font-size: 11px; " +
            "-fx-font-weight: bold;"
        );

        header.getChildren().addAll(radio, titleLabel, badgeLabel);

        // Description
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #666; -fx-padding: 5 0 0 28;");
        descLabel.setWrapText(true);

        optionBox.getChildren().addAll(header, descLabel);

        // Store reference
        if (type == SubmissionTypeConstants.PRIDE || type == SubmissionTypeConstants.PARTIAL) {
            prideRadio = radio;
        } else if (type == SubmissionTypeConstants.AFFINITY) {
            affinityRadio = radio;
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
                case PRIDE, PARTIAL, COMPLETE -> prideRadio.setSelected(true);
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
