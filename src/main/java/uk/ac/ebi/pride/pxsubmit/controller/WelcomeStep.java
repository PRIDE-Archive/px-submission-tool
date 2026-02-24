package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;

/**
 * Welcome step that introduces the submission process and file requirements.
 * Educates users on what files they need before starting.
 *
 * Based on PRIDE Submission Guidelines:
 * - RAW files (Mandatory): Instrument raw data
 * - ANALYSIS files (Mandatory): Search engine outputs
 * - FASTA database (Optional): Protein sequence database
 * - STANDARD files (Recommended): mzIdentML, mzTab for COMPLETE submission
 * - PEAK lists (Optional): MGF, DTA files
 * - SDRF (Recommended): Sample metadata
 */
public class WelcomeStep extends AbstractWizardStep {

    private CheckBox trainingModeCheckbox;
    private CheckBox licenseCheckBox;

    public WelcomeStep(SubmissionModel model) {
        super("welcome",
              "Welcome to PRIDE Submission",
              "Prepare your proteomics data for submission to PRIDE Archive",
              model);
    }

    @Override
    protected Parent createContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(12);
        content.setPadding(new Insets(10, 15, 10, 15));
        content.setAlignment(Pos.TOP_CENTER);

        // Introduction
        VBox introSection = createSection("About PRIDE Submissions",
            "PRIDE Archive is a centralized, standards-compliant, public data repository for " +
            "mass spectrometry-based proteomics data. Before you begin, please ensure you have " +
            "the required files ready.");

        // File Categories Guide
        VBox categoriesSection = createFileCategories();

        // Checklist
        VBox checklistSection = createChecklist();

        // License acceptance
        VBox licenseSection = createLicenseSection();

        // Training Mode + Help links side by side
        HBox bottomRow = new HBox(20);
        VBox trainingSection = createTrainingSection();
        VBox helpSection = createHelpSection();
        HBox.setHgrow(trainingSection, Priority.ALWAYS);
        HBox.setHgrow(helpSection, Priority.ALWAYS);
        bottomRow.getChildren().addAll(trainingSection, helpSection);

        content.getChildren().addAll(
            introSection,
            categoriesSection,
            checklistSection,
            new Separator(),
            licenseSection,
            new Separator(),
            bottomRow
        );

        scrollPane.setContent(content);
        return scrollPane;
    }

    private VBox createSection(String title, String description) {
        VBox section = new VBox(4);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        section.getChildren().addAll(titleLabel, descLabel);
        return section;
    }

    private VBox createFileCategories() {
        VBox section = new VBox(6);

        Label titleLabel = new Label("File Categories");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label descLabel = new Label("Your submission should include the following types of files:");
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        // Grid of file categories
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(4, 0, 0, 0));

        // RAW Files
        grid.add(createCategoryCard(
            "RAW Files",
            "MANDATORY",
            "#0066cc",
            "Instrument raw data files",
            ".raw, .wiff, .d, .mzML, .mzXML"
        ), 0, 0);

        // ANALYSIS Files (with Standard files info)
        grid.add(createCategoryCard(
            "ANALYSIS Files",
            "MANDATORY",
            "#28a745",
            "Search engine / analysis tool outputs. Standard formats (mzIdentML, mzTab) also accepted.",
            "MaxQuant, DIA-NN, FragPipe, Mascot, .mzid, .mzTab"
        ), 1, 0);

        // Reference Database
        grid.add(createCategoryCard(
            "Reference Database",
            "OPTIONAL",
            "#17a2b8",
            "Protein sequence database used",
            ".fasta, .fa, .faa"
        ), 2, 0);

        // Other Files
        grid.add(createCategoryCard(
            "Other Files",
            "OPTIONAL",
            "#6f42c1",
            "Any additional supplementary files",
            "Documentation, scripts, etc."
        ), 0, 1);

        // PEAK Lists
        grid.add(createCategoryCard(
            "PEAK Lists",
            "OPTIONAL",
            "#fd7e14",
            "Peak list files if referenced",
            ".mgf, .dta, .pkl, .ms2"
        ), 1, 1);

        // SDRF
        grid.add(createCategoryCard(
            "SDRF Metadata",
            "RECOMMENDED",
            "#e83e8c",
            "Sample and experimental design",
            "sdrf.tsv"
        ), 2, 1);

        section.getChildren().addAll(titleLabel, descLabel, grid);
        return section;
    }

    private VBox createCategoryCard(String name, String status, String color, String description, String examples) {
        VBox card = new VBox(3);
        card.setPadding(new Insets(8));
        card.setMinWidth(180);
        card.setMaxWidth(260);

        String bgColor = status.equals("MANDATORY") ? "#fff3cd" :
                        status.equals("RECOMMENDED") ? "#e7f3ff" : "#f8f9fa";

        card.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-border-color: %s; " +
            "-fx-border-width: 2 2 2 4; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4;",
            bgColor, color));

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label statusLabel = new Label(status);
        statusLabel.setStyle(String.format(
            "-fx-font-size: 10px; -fx-font-weight: bold; " +
            "-fx-background-color: %s; -fx-text-fill: white; " +
            "-fx-padding: 2 6; -fx-background-radius: 8;",
            color));

        header.getChildren().addAll(nameLabel, statusLabel);

        // Description
        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        // Examples
        Label exLabel = new Label(examples);
        exLabel.setWrapText(true);
        exLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 10px; -fx-font-style: italic;");

        card.getChildren().addAll(header, descLabel, exLabel);
        return card;
    }

    private VBox createChecklist() {
        VBox section = new VBox(4);

        Label titleLabel = new Label("Before You Begin");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        VBox checklistBox = new VBox(3);
        checklistBox.setPadding(new Insets(0, 0, 0, 10));

        String[] items = {
            "I have all my RAW instrument files ready",
            "I have the FASTA database used for the search",
            "I have my analysis/search results (MaxQuant, DIA-NN, etc.)",
            "I know the sample metadata (organism, tissue, instrument)"
        };

        for (String item : items) {
            HBox itemBox = new HBox(8);
            itemBox.setAlignment(Pos.CENTER_LEFT);
            Label checkIcon = new Label("\u2713"); // Checkmark
            checkIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #28a745;");
            Label itemLabel = new Label(item);
            itemBox.getChildren().addAll(checkIcon, itemLabel);
            checklistBox.getChildren().add(itemBox);
        }

        // Add PRIDE account item with registration link
        HBox accountBox = new HBox(4);
        accountBox.setAlignment(Pos.CENTER_LEFT);
        Label accountCheckIcon = new Label("\u2713");
        accountCheckIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #28a745;");
        Label accountText = new Label("I have a PRIDE account -");
        Hyperlink registerLink = new Hyperlink("register here");
        registerLink.setPadding(new Insets(0));
        registerLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride/register"));
        Label accountTextEnd = new Label("if you don't have one");
        accountBox.getChildren().addAll(accountCheckIcon, accountText, registerLink, accountTextEnd);
        checklistBox.getChildren().add(accountBox);

        section.getChildren().addAll(titleLabel, checklistBox);
        return section;
    }

    private VBox createLicenseSection() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10, 15, 10, 15));
        box.setStyle(
            "-fx-background-color: #f0f7ff; " +
            "-fx-border-color: #b3d4fc; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6;");

        Label titleLabel = new Label("Dataset License Agreement");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        HBox checkboxRow = new HBox(4);
        checkboxRow.setAlignment(Pos.CENTER_LEFT);

        licenseCheckBox = new CheckBox("I accept the");
        licenseCheckBox.setStyle("-fx-font-size: 12px;");

        Hyperlink licenseLink = new Hyperlink("PRIDE Archive dataset license (CC0)");
        licenseLink.setStyle("-fx-font-size: 12px;");
        licenseLink.setPadding(new Insets(0));
        licenseLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride/markdownpage/usagepolicy"));

        Label andLabel = new Label("and have read the");
        andLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");

        Hyperlink policyLink = new Hyperlink("PRIDE data policy");
        policyLink.setStyle("-fx-font-size: 12px;");
        policyLink.setPadding(new Insets(0));
        policyLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride/markdownpage/datapolicy"));

        checkboxRow.getChildren().addAll(licenseCheckBox, licenseLink, andLabel, policyLink);

        Label requiredLabel = new Label("You must accept the license to proceed with your submission.");
        requiredLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px; -fx-font-style: italic;");

        box.getChildren().addAll(titleLabel, checkboxRow, requiredLabel);
        return box;
    }

    private VBox createTrainingSection() {
        VBox section = new VBox(6);

        Label titleLabel = new Label("Test Mode");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        trainingModeCheckbox = new CheckBox("Enable Test Mode");
        trainingModeCheckbox.setStyle("-fx-font-weight: bold;");

        // Add tooltip with description instead of showing it directly
        Tooltip testModeTooltip = new Tooltip(
            "Test mode allows you to explore the submission process without actually " +
            "uploading files. Perfect for learning or testing. No data will be sent to PRIDE.");
        testModeTooltip.setWrapText(true);
        testModeTooltip.setMaxWidth(300);
        trainingModeCheckbox.setTooltip(testModeTooltip);

        // Warning box
        HBox warningBox = new HBox(8);
        warningBox.setPadding(new Insets(6, 10, 6, 10));
        warningBox.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #ffc107; -fx-border-radius: 4; -fx-background-radius: 4;");
        warningBox.setAlignment(Pos.CENTER_LEFT);

        Label warningIcon = new Label("\u26A0");
        warningIcon.setStyle("-fx-text-fill: #856404; -fx-font-size: 16px;");

        Label warningText = new Label("Test mode submissions will NOT be stored in PRIDE Archive.");
        warningText.setStyle("-fx-text-fill: #856404; -fx-font-size: 12px;");

        warningBox.getChildren().addAll(warningIcon, warningText);
        warningBox.visibleProperty().bind(trainingModeCheckbox.selectedProperty());
        warningBox.managedProperty().bind(trainingModeCheckbox.selectedProperty());

        section.getChildren().addAll(titleLabel, trainingModeCheckbox, warningBox);
        return section;
    }

    private VBox createHelpSection() {
        VBox section = new VBox(6);

        Label titleLabel = new Label("Need Help?");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        VBox linksBox = new VBox(2);

        Hyperlink guidelinesLink = new Hyperlink("PRIDE Submission Guidelines");
        guidelinesLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride/markdownpage/submitdatapage"));

        Hyperlink formatLink = new Hyperlink("Supported File Formats");
        formatLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride/markdownpage/pridefileformats"));

        Hyperlink sdrfLink = new Hyperlink("SDRF Format Guide");
        sdrfLink.setOnAction(e -> openUrl("https://github.com/bigbio/proteomics-sample-metadata"));

        Hyperlink contactLink = new Hyperlink("Contact PRIDE Support: pride-support@ebi.ac.uk");
        contactLink.setOnAction(e -> openUrl("mailto:pride-support@ebi.ac.uk"));

        linksBox.getChildren().addAll(guidelinesLink, formatLink, sdrfLink, contactLink);

        section.getChildren().addAll(titleLabel, linksBox);
        return section;
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            logger.warn("Could not open URL: {}", url, e);
        }
    }

    @Override
    protected void initializeStep() {
        // Bind training mode checkbox to model
        trainingModeCheckbox.selectedProperty().bindBidirectional(model.trainingModeProperty());

        // Valid only when license is accepted
        valid.bind(licenseCheckBox.selectedProperty());
    }

    @Override
    protected void onStepEntering() {
        // Update training mode state
        trainingModeCheckbox.setSelected(model.isTrainingMode());
    }

    @Override
    public boolean showBackButton() {
        return false; // First step, no back button
    }

    @Override
    public String getNextButtonText() {
        return "Get Started";
    }
}
