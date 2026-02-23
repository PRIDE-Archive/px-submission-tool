package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import uk.ac.ebi.pride.archive.dataprovider.utils.SubmissionTypeConstants;
import uk.ac.ebi.pride.archive.submission.model.project.ProjectDetail;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.service.ApiService;
import uk.ac.ebi.pride.pxsubmit.service.ServiceFactory;

/**
 * Step for selecting the submission type.
 * Options: PRIDE (mass spectrometry), Affinity Proteomics, or Resubmission.
 *
 * Note: The COMPLETE/PARTIAL distinction is handled automatically based on file types.
 * If the submission includes STANDARD file formats (mzIdentML, mzTab),
 * it will be registered as a complete ProteomeXchange submission.
 */
public class SubmissionTypeStep extends AbstractWizardStep {

    private ToggleGroup submissionTypeGroup;
    private RadioButton prideRadio;
    private RadioButton affinityRadio;
    private RadioButton resubmissionRadio;

    // Resubmission UI
    private VBox resubmissionDetailBox;
    private ComboBox<String> projectComboBox;
    private HBox projectLoadingBox;
    private Label resubmissionErrorLabel;
    private boolean projectsLoaded = false;

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
            "pride",
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
            "affinity",
            false
        );

        // Resubmission
        VBox resubmissionBox = createResubmissionOption();

        // Links section
        HBox linksBox = new HBox(20);
        linksBox.setAlignment(Pos.CENTER);
        linksBox.setPadding(new Insets(20, 0, 0, 0));

        Hyperlink guidelinesLink = new Hyperlink("Submission Guidelines");
        guidelinesLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride/help/archive/submission"));

        Hyperlink prideLink = new Hyperlink("About PRIDE");
        prideLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride"));

        linksBox.getChildren().addAll(guidelinesLink, prideLink);

        root.getChildren().addAll(introLabel, prideBox, affinityBox, resubmissionBox, linksBox);

        return root;
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

    private VBox createResubmissionOption() {
        VBox optionBox = new VBox(8);
        optionBox.setPadding(new Insets(15));
        optionBox.setStyle(
            "-fx-background-color: #f8f9fa; " +
            "-fx-border-color: #dee2e6; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8;"
        );

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        resubmissionRadio = new RadioButton();
        resubmissionRadio.setToggleGroup(submissionTypeGroup);
        resubmissionRadio.setUserData("resubmission");

        Label titleLabel = new Label("Resubmission");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label badgeLabel = new Label("Resub");
        badgeLabel.setStyle(
            "-fx-background-color: #28a745; " +
            "-fx-text-fill: white; " +
            "-fx-padding: 2 8; " +
            "-fx-background-radius: 4; " +
            "-fx-font-size: 11px; " +
            "-fx-font-weight: bold;"
        );

        header.getChildren().addAll(resubmissionRadio, titleLabel, badgeLabel);

        // Description
        Label descLabel = new Label("Resubmit files to an existing private PRIDE project");
        descLabel.setStyle("-fx-text-fill: #666; -fx-padding: 5 0 0 28;");
        descLabel.setWrapText(true);

        // Project selection detail (hidden until resubmission radio selected)
        resubmissionDetailBox = new VBox(8);
        resubmissionDetailBox.setPadding(new Insets(10, 0, 0, 28));
        resubmissionDetailBox.setVisible(false);
        resubmissionDetailBox.setManaged(false);

        Label projectLabel = new Label("Select project:");
        projectComboBox = new ComboBox<>();
        projectComboBox.setPromptText("Select a project...");
        projectComboBox.setPrefWidth(300);
        projectComboBox.setDisable(true);

        projectLoadingBox = new HBox(8);
        projectLoadingBox.setAlignment(Pos.CENTER_LEFT);
        ProgressIndicator projectSpinner = new ProgressIndicator();
        projectSpinner.setPrefSize(16, 16);
        Label loadingLabel = new Label("Loading your projects...");
        loadingLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        projectLoadingBox.getChildren().addAll(projectSpinner, loadingLabel);
        projectLoadingBox.setVisible(false);
        projectLoadingBox.setManaged(false);

        resubmissionErrorLabel = new Label();
        resubmissionErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        resubmissionErrorLabel.setVisible(false);
        resubmissionErrorLabel.setManaged(false);

        resubmissionDetailBox.getChildren().addAll(projectLabel, projectComboBox, projectLoadingBox, resubmissionErrorLabel);

        optionBox.getChildren().addAll(header, descLabel, resubmissionDetailBox);

        // Click anywhere in box to select
        optionBox.setOnMouseClicked(e -> resubmissionRadio.setSelected(true));

        optionBox.setOnMouseEntered(e ->
            optionBox.setStyle(
                "-fx-background-color: #e9ecef; " +
                "-fx-border-color: #28a745; " +
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
                String optionId = (String) newVal.getUserData();
                handleOptionChange(optionId);
            }
            valid.set(newVal != null);
        });

        // Set initial value in model
        Toggle selected = submissionTypeGroup.getSelectedToggle();
        if (selected != null) {
            handleOptionChange((String) selected.getUserData());
        }
    }

    private void handleOptionChange(String optionId) {
        switch (optionId) {
            case "pride" -> {
                model.setSubmissionType(SubmissionTypeConstants.PRIDE);
                model.setResubmissionMode(false);
                model.getSubmission().getProjectMetaData().setResubmissionPxAccession(null);
                resubmissionDetailBox.setVisible(false);
                resubmissionDetailBox.setManaged(false);
                logger.info("Submission type selected: PRIDE");
            }
            case "affinity" -> {
                model.setSubmissionType(SubmissionTypeConstants.AFFINITY);
                model.setResubmissionMode(false);
                model.getSubmission().getProjectMetaData().setResubmissionPxAccession(null);
                resubmissionDetailBox.setVisible(false);
                resubmissionDetailBox.setManaged(false);
                logger.info("Submission type selected: AFFINITY");
            }
            case "resubmission" -> {
                model.setSubmissionType(SubmissionTypeConstants.PRIDE);
                model.setResubmissionMode(true);
                resubmissionDetailBox.setVisible(true);
                resubmissionDetailBox.setManaged(true);
                if (!projectsLoaded) {
                    loadProjects();
                }
                logger.info("Submission type selected: Resubmission");
            }
        }
    }

    private void loadProjects() {
        projectLoadingBox.setVisible(true);
        projectLoadingBox.setManaged(true);
        projectComboBox.setDisable(true);
        projectComboBox.getItems().clear();
        resubmissionErrorLabel.setVisible(false);
        resubmissionErrorLabel.setManaged(false);

        String currentUser = model.getUserName();
        String currentPass = model.getPassword();
        if (currentUser == null || currentPass == null) {
            resubmissionErrorLabel.setText("Not logged in. Please go back and log in first.");
            resubmissionErrorLabel.setVisible(true);
            resubmissionErrorLabel.setManaged(true);
            projectLoadingBox.setVisible(false);
            projectLoadingBox.setManaged(false);
            return;
        }

        logger.debug("Loading projects for user: {}", currentUser);
        ApiService apiService = ServiceFactory.getInstance().createApiService(currentUser, currentPass);
        apiService.getSubmissionDetails()
            .thenAccept(projectDetailList -> Platform.runLater(() -> {
                projectLoadingBox.setVisible(false);
                projectLoadingBox.setManaged(false);

                if (projectDetailList.getProjectDetails().isEmpty()) {
                    resubmissionErrorLabel.setText("No private projects found, or a resubmission is already pending.");
                    resubmissionErrorLabel.setVisible(true);
                    resubmissionErrorLabel.setManaged(true);
                    projectComboBox.setPromptText("No projects available");
                } else {
                    for (ProjectDetail pd : projectDetailList.getProjectDetails()) {
                        projectComboBox.getItems().add(pd.getAccession());
                    }
                    projectComboBox.setPromptText("Select a project...");
                    projectComboBox.setDisable(false);
                    projectsLoaded = true;
                }
                apiService.shutdown();
            }))
            .exceptionally(ex -> {
                logger.error("Failed to load projects for resubmission", ex);
                Platform.runLater(() -> {
                    projectLoadingBox.setVisible(false);
                    projectLoadingBox.setManaged(false);
                    String msg = "Could not load projects. Please check your credentials and try again.";
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause.getMessage() != null && cause.getMessage().contains("403")) {
                        msg = "Access denied. Please verify your account has resubmission permissions.";
                    } else if (cause.getMessage() != null && cause.getMessage().contains("401")) {
                        msg = "Authentication failed. Please go back and re-enter your credentials.";
                    }
                    resubmissionErrorLabel.setText(msg);
                    resubmissionErrorLabel.setVisible(true);
                    resubmissionErrorLabel.setManaged(true);
                });
                apiService.shutdown();
                return null;
            });
    }

    @Override
    protected void onStepEntering() {
        // Restore previous selection if any
        if (model.isResubmissionMode()) {
            resubmissionRadio.setSelected(true);
        } else {
            SubmissionTypeConstants currentType = model.getSubmissionType();
            if (currentType != null) {
                switch (currentType) {
                    case PRIDE, PARTIAL, COMPLETE -> prideRadio.setSelected(true);
                    case AFFINITY -> affinityRadio.setSelected(true);
                }
            }
        }

        // Reset project loading if coming back after logout
        if (!model.isLoggedIn()) {
            projectsLoaded = false;
            projectComboBox.getItems().clear();
            projectComboBox.setDisable(true);
        }
    }

    @Override
    public boolean validate() {
        Toggle selected = submissionTypeGroup.getSelectedToggle();
        if (selected == null) return false;

        String optionId = (String) selected.getUserData();
        if ("resubmission".equals(optionId)) {
            String selectedAccession = projectComboBox.getValue();
            if (selectedAccession == null || selectedAccession.isEmpty()) {
                resubmissionErrorLabel.setText("Please select a project for resubmission");
                resubmissionErrorLabel.setVisible(true);
                resubmissionErrorLabel.setManaged(true);
                return false;
            }
            model.setResubmissionMode(true);
            model.getSubmission().getProjectMetaData().setResubmissionPxAccession(selectedAccession);
            logger.info("Resubmission mode enabled for project: {}", selectedAccession);
        }
        return true;
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            logger.error("Failed to open URL: {}", url, e);
        }
    }
}
