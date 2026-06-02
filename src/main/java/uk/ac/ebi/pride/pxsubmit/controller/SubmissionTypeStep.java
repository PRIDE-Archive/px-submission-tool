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
 * Options: PRIDE (mass spectrometry) or Affinity Proteomics.
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
              "Submission Type",
              "Choose the type of proteomics data you want to submit",
              model);
    }

    @Override
    protected Parent createContent() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);

        // Radio button group
        submissionTypeGroup = new ToggleGroup();

        // Mass Spectrometry Submission
        VBox prideBox = createSubmissionOption(
            "Mass Spectrometry Proteomics",
            "MS",
            "Submit mass spectrometry-based proteomics data:\n\n" +
            "  \u2022 RAW files from your mass spectrometer\n" +
            "  \u2022 Analysis outputs (MaxQuant, DIA-NN, FragPipe, Spectronaut, etc.)\n" +
            "  \u2022 Protein sequence databases (FASTA)\n" +
            "  \u2022 Sample metadata (SDRF)",
            "pride",
            true
        );

        // Affinity Proteomics Submission
        VBox affinityBox = createSubmissionOption(
            "Affinity Proteomics",
            "Affinity",
            "Submit affinity-based proteomics data:\n\n" +
            "  \u2022 SomaScan (ADAT files)\n" +
            "  \u2022 Olink (NPX or Parquet files)\n" +
            "  \u2022 Other antibody-based or aptamer-based assays",
            "affinity",
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

        root.getChildren().addAll(prideBox, affinityBox, linksBox);

        // Wrap in a scroll pane so the options remain reachable on short windows.
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        return scrollPane;
    }

    private VBox createSubmissionOption(String title, String badge, String description,
                                         String optionId, boolean selected) {
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
        radio.setUserData(optionId);
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
        if ("pride".equals(optionId)) {
            prideRadio = radio;
        } else if ("affinity".equals(optionId)) {
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
        // Listen for selection changes to update model
        submissionTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String optionId = (String) newVal.getUserData();
                handleOptionChange(optionId);
            }
            updateValidity();
        });

        // Set initial value in model
        Toggle selected = submissionTypeGroup.getSelectedToggle();
        if (selected != null) {
            handleOptionChange((String) selected.getUserData());
        }

        updateValidity();
    }

    private void updateValidity() {
        Toggle selected = submissionTypeGroup.getSelectedToggle();
        valid.set(selected != null);
    }

    private void handleOptionChange(String optionId) {
        switch (optionId) {
            case "pride" -> {
                model.setSubmissionType(SubmissionTypeConstants.PRIDE);
                logger.info("Submission type selected: PRIDE");
            }
            case "affinity" -> {
                model.setSubmissionType(SubmissionTypeConstants.AFFINITY);
                logger.info("Submission type selected: AFFINITY");
            }
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

    @Override
    public boolean validate() {
        return submissionTypeGroup.getSelectedToggle() != null;
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            logger.error("Failed to open URL: {}", url, e);
        }
    }
}
