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
 * - FASTA database (Mandatory): Protein sequence database
 * - STANDARD files (Recommended): mzIdentML, mzTab for COMPLETE submission
 * - PEAK lists (Optional): MGF, DTA files
 * - SDRF (Recommended): Sample metadata
 */
public class WelcomeStep extends AbstractWizardStep {

    private CheckBox trainingModeCheckbox;

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
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
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

        // Training Mode
        VBox trainingSection = createTrainingSection();

        // Help links
        VBox helpSection = createHelpSection();

        content.getChildren().addAll(
            introSection,
            new Separator(),
            categoriesSection,
            new Separator(),
            checklistSection,
            new Separator(),
            trainingSection,
            new Separator(),
            helpSection
        );

        scrollPane.setContent(content);
        return scrollPane;
    }

    private VBox createSection(String title, String description) {
        VBox section = new VBox(8);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #666;");

        section.getChildren().addAll(titleLabel, descLabel);
        return section;
    }

    private VBox createFileCategories() {
        VBox section = new VBox(15);

        Label titleLabel = new Label("File Categories");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label descLabel = new Label("Your submission should include the following types of files:");
        descLabel.setStyle("-fx-text-fill: #666;");

        // Grid of file categories
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

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
            "MANDATORY",
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
        VBox card = new VBox(5);
        card.setPadding(new Insets(12));
        card.setMinWidth(200);
        card.setMaxWidth(250);

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
        VBox section = new VBox(12);

        Label titleLabel = new Label("Before You Begin");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox checklistBox = new VBox(6);
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

    private VBox createTrainingSection() {
        VBox section = new VBox(10);

        Label titleLabel = new Label("Test Mode");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

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
        HBox warningBox = new HBox(10);
        warningBox.setPadding(new Insets(10));
        warningBox.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #ffc107; -fx-border-radius: 4; -fx-background-radius: 4;");
        warningBox.setAlignment(Pos.CENTER_LEFT);

        Label warningIcon = new Label("\u26A0");
        warningIcon.setStyle("-fx-text-fill: #856404; -fx-font-size: 16px;");

        Label warningText = new Label("Test mode submissions will NOT be stored in PRIDE Archive.");
        warningText.setStyle("-fx-text-fill: #856404;");

        warningBox.getChildren().addAll(warningIcon, warningText);
        warningBox.visibleProperty().bind(trainingModeCheckbox.selectedProperty());
        warningBox.managedProperty().bind(trainingModeCheckbox.selectedProperty());

        section.getChildren().addAll(titleLabel, trainingModeCheckbox, warningBox);
        return section;
    }

    private VBox createHelpSection() {
        VBox section = new VBox(10);

        Label titleLabel = new Label("Need Help?");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox linksBox = new VBox(5);

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

        // This step is always valid
        valid.set(true);
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
