package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.service.OlsService;
import uk.ac.ebi.pride.pxsubmit.service.ServiceFactory;
import uk.ac.ebi.pride.pxsubmit.service.ai.KeywordSuggestionService;
import uk.ac.ebi.pride.pxsubmit.view.dialog.SettingsDialog;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.ac.ebi.pride.pxsubmit.service.OlsService.OlsOntology;
import uk.ac.ebi.pride.pxsubmit.view.component.ChipInput;
import uk.ac.ebi.pride.pxsubmit.view.component.OlsAutocomplete;
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
    private OlsAutocomplete experimentTypeField;
    private OlsAutocomplete softwareField;

    // Character counters
    private Label titleCounter;
    private Label descriptionCounter;

    // Project Tags
    private MenuButton projectTagsMenuButton;
    private FlowPane selectedTagsPane;
    private final List<CheckMenuItem> tagMenuItems = new ArrayList<>();

    // Crosslinking warning banner
    private static final String CROSSLINK_ACCESSION = "PRIDE:0000430";
    private VBox crosslinkBanner;
    private VBox form; // reference for banner insertion

    // AI keyword suggestion UI
    private Button suggestKeywordsButton;
    private ProgressIndicator aiProgressIndicator;
    private Label aiStatusLabel;
    private FlowPane aiSuggestionsPane;
    private VBox aiSuggestionBox;
    private final Set<String> addedSuggestions = new HashSet<>();

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
    public boolean canSkip() {
        // Skip this step during resubmission - project metadata already exists on the server
        return model.isResubmissionMode();
    }

    @Override
    protected Parent createContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        form = new VBox(20);
        form.setPadding(new Insets(20));

        // Crosslinking warning banner (hidden by default, shown when detected)
        crosslinkBanner = createCrosslinkBanner();
        crosslinkBanner.setVisible(false);
        crosslinkBanner.setManaged(false);

        // Project Title
        VBox titleSection = createFieldSection("Project Title",
            "A short descriptive title for your project (5-500 characters)", true);
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
        VBox descSection = createFieldSection("Project Description",
            "Detailed description of your project (20-5000 characters)", true);
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

        // Experiment Type
        VBox experimentTypeSection = createFieldSection("Experiment Type",
            "Select the type of mass spectrometry experiment", true);
        experimentTypeField = new OlsAutocomplete(OlsOntology.PRIDE);
        experimentTypeField.setPromptText("Search experiment type (e.g., Bottom-up, DIA, DDA)...");
        experimentTypeField.setCommonTerms(OlsService.getCommonExperimentTypes());
        experimentTypeField.setOnTermSelected(term -> model.addExperimentMethod(term));
        experimentTypeField.setOnTermRemoved(term -> model.removeExperimentMethod(term));
        experimentTypeSection.getChildren().add(experimentTypeField);
        experimentTypeSection.getChildren().add(createQuickSelectPane(OlsService.getCommonExperimentTypes(), experimentTypeField));

        // Software / Analysis Tools
        VBox softwareSection = createFieldSection("Software / Analysis Tools",
            "Select the software used for data analysis", true);
        softwareField = new OlsAutocomplete(OlsOntology.MS);
        softwareField.setPromptText("Search software (e.g., MaxQuant, DIA-NN, FragPipe)...");
        softwareField.setCommonTerms(OlsService.getCommonSoftware());
        softwareField.setOnTermSelected(term -> model.addSoftware(term));
        softwareField.setOnTermRemoved(term -> model.removeSoftware(term));
        softwareSection.getChildren().add(softwareField);
        softwareSection.getChildren().add(createQuickSelectPane(OlsService.getCommonSoftware(), softwareField));

        // Sample Processing Protocol
        VBox sampleSection = createFieldSection("Sample Processing Protocol",
            "Describe how samples were prepared for analysis", true);
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
        VBox dataSection = createFieldSection("Data Processing Protocol",
            "Describe how data was analyzed", true);
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

        // Project Tags (optional)
        VBox projectTagsSection = createProjectTagsSection();

        // Keywords (moved to bottom so users fill context first for AI suggestions)
        VBox keywordsSection = createFieldSection("Keywords",
            "Type a keyword and press Enter to add it. Click \u2715 to remove.", true);
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

        // AI keyword suggestion UI
        aiSuggestionBox = createAiSuggestionBox();
        keywordsSection.getChildren().add(aiSuggestionBox);

        // Validation feedback
        validationFeedback = new ValidationFeedback();

        form.getChildren().addAll(
            crosslinkBanner,
            titleSection,
            descSection,
            sampleSection,
            dataSection,
            experimentTypeSection,
            softwareSection,
            new Separator(),
            projectTagsSection,
            keywordsSection,
            validationFeedback
        );

        scrollPane.setContent(form);
        return scrollPane;
    }

    /**
     * Create the inline crosslinking warning banner.
     * Styled as a modern notification card with icon, message, and clickable links.
     */
    private VBox createCrosslinkBanner() {
        VBox banner = new VBox(8);
        banner.setPadding(new Insets(14));
        banner.setStyle(
            "-fx-background-color: #fff8e1; " +
            "-fx-border-color: #f9a825; " +
            "-fx-border-width: 0 0 0 4; " +
            "-fx-border-radius: 0 6 6 0; " +
            "-fx-background-radius: 0 6 6 0;");

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("\u26A0");
        icon.setStyle("-fx-font-size: 18px; -fx-text-fill: #f9a825;");

        Label title = new Label("Crosslinking Dataset Detected");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #6d4c00;");

        headerRow.getChildren().addAll(icon, title);

        Label message = new Label(
            "We detected this may be a crosslinking dataset. " +
            "Please ensure your files and metadata meet the requirements " +
            "for the PRIDE Crosslinking Resource.");
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #6d4c00; -fx-font-size: 12px;");

        HBox linksRow = new HBox(16);
        linksRow.setPadding(new Insets(4, 0, 0, 0));

        Hyperlink guidelinesLink = new Hyperlink("Submission Guidelines");
        guidelinesLink.setStyle("-fx-text-fill: #0066cc; -fx-font-size: 12px;");
        guidelinesLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride/markdownpage/crosslinking"));

        Hyperlink resourceLink = new Hyperlink("PRIDE Crosslinking Resource");
        resourceLink.setStyle("-fx-text-fill: #0066cc; -fx-font-size: 12px;");
        resourceLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride/archive/crosslinking"));

        linksRow.getChildren().addAll(guidelinesLink, resourceLink);

        // Dismiss button
        Button dismissBtn = new Button("\u2715");
        dismissBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #6d4c00; " +
            "-fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0 4;");
        dismissBtn.setOnAction(e -> {
            banner.setVisible(false);
            banner.setManaged(false);
        });

        // Title row with dismiss on the right
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().addAll(headerRow, spacer, dismissBtn);

        banner.getChildren().addAll(topRow, message, linksRow);
        return banner;
    }

    /**
     * Check if the current metadata indicates a crosslinking dataset.
     * Detection: experiment type CV accession PRIDE:0000430, or
     * "crosslink"/"cross-link" in title, keywords, description, or protocols.
     */
    private boolean isCrosslinkDataset() {
        // Check experiment type accession
        boolean hasCrosslinkType = experimentTypeField.getSelectedTerms().stream()
            .anyMatch(t -> CROSSLINK_ACCESSION.equals(t.getAccession()));
        if (hasCrosslinkType) return true;

        // Check text fields for crosslinking keywords
        String[] fieldsToCheck = {
            titleField.getText(),
            keywordsInput.getText(),
            descriptionArea.getText(),
            sampleProtocolArea.getText(),
            dataProtocolArea.getText()
        };
        for (String text : fieldsToCheck) {
            if (text != null) {
                String lower = text.toLowerCase();
                if (lower.contains("crosslink") || lower.contains("cross-link")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Show/hide the crosslinking banner reactively based on experiment type selection.
     */
    private void updateCrosslinkBanner() {
        boolean detected = experimentTypeField.getSelectedTerms().stream()
            .anyMatch(t -> CROSSLINK_ACCESSION.equals(t.getAccession()));
        crosslinkBanner.setVisible(detected);
        crosslinkBanner.setManaged(detected);
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            logger.error("Failed to open URL: {}", url, e);
        }
    }

    private VBox createAiSuggestionBox() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(8, 0, 0, 0));

        // Button + progress indicator row
        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        suggestKeywordsButton = new Button("Suggest Keywords with AI");
        suggestKeywordsButton.setStyle(
            "-fx-background-color: #0066cc; " +
            "-fx-text-fill: white; " +
            "-fx-padding: 6 16; " +
            "-fx-background-radius: 4; " +
            "-fx-cursor: hand;");
        suggestKeywordsButton.setOnAction(e -> requestAiSuggestions());

        aiProgressIndicator = new ProgressIndicator();
        aiProgressIndicator.setPrefSize(20, 20);
        aiProgressIndicator.setVisible(false);
        aiProgressIndicator.setManaged(false);

        aiStatusLabel = new Label();
        aiStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        buttonRow.getChildren().addAll(suggestKeywordsButton, aiProgressIndicator, aiStatusLabel);

        // Suggestions pane
        aiSuggestionsPane = new FlowPane();
        aiSuggestionsPane.setHgap(6);
        aiSuggestionsPane.setVgap(6);
        aiSuggestionsPane.setPadding(new Insets(4, 0, 0, 0));

        box.getChildren().addAll(buttonRow, aiSuggestionsPane);
        return box;
    }

    private void requestAiSuggestions() {
        // Validate that title and description are filled
        String title = titleField.getText();
        String desc = descriptionArea.getText();
        if (isNullOrEmpty(title) || isNullOrEmpty(desc)) {
            aiStatusLabel.setText("Please fill in the title and description first.");
            aiStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #dc3545;");
            return;
        }

        // Check AI is enabled
        boolean aiEnabled = SettingsDialog.getBooleanPreference("ai.enabled", true);
        if (!aiEnabled) {
            aiStatusLabel.setText("AI assistant is disabled in Settings.");
            aiStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #dc3545;");
            return;
        }

        // Create service and check configuration
        KeywordSuggestionService service = ServiceFactory.getInstance().createKeywordSuggestionService();
        if (!service.isConfigured()) {
            aiStatusLabel.setText("Please configure the AI API URL and key in Settings.");
            aiStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #dc3545;");
            return;
        }

        // Show progress
        suggestKeywordsButton.setDisable(true);
        aiProgressIndicator.setVisible(true);
        aiProgressIndicator.setManaged(true);
        aiStatusLabel.setText("Requesting suggestions...");
        aiStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        aiSuggestionsPane.getChildren().clear();
        addedSuggestions.clear();

        // Gather experiment types
        List<String> experimentTypes = experimentTypeField.getSelectedTerms().stream()
                .map(CvParam::getName)
                .toList();

        service.suggestKeywords(
                title, desc,
                sampleProtocolArea.getText(),
                dataProtocolArea.getText(),
                experimentTypes
        ).thenAccept(suggestions -> Platform.runLater(() -> {
            aiProgressIndicator.setVisible(false);
            aiProgressIndicator.setManaged(false);
            suggestKeywordsButton.setDisable(false);

            if (suggestions.isEmpty()) {
                aiStatusLabel.setText("No suggestions returned. Check your API configuration.");
                aiStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #dc3545;");
                return;
            }

            aiStatusLabel.setText("Click a suggestion to add it as a keyword:");
            aiStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #28a745;");

            for (String keyword : suggestions) {
                Label chip = createSuggestionChip(keyword);
                aiSuggestionsPane.getChildren().add(chip);
            }
        }));
    }

    private Label createSuggestionChip(String keyword) {
        Label chip = new Label(keyword);
        chip.setStyle(
            "-fx-font-size: 11px; " +
            "-fx-padding: 4 10; " +
            "-fx-background-color: #e8f4fd; " +
            "-fx-text-fill: #0066cc; " +
            "-fx-background-radius: 12; " +
            "-fx-border-color: #b3d9f2; " +
            "-fx-border-radius: 12; " +
            "-fx-cursor: hand;");
        chip.setOnMouseEntered(e -> {
            if (!addedSuggestions.contains(keyword)) {
                chip.setStyle(
                    "-fx-font-size: 11px; " +
                    "-fx-padding: 4 10; " +
                    "-fx-background-color: #0066cc; " +
                    "-fx-text-fill: white; " +
                    "-fx-background-radius: 12; " +
                    "-fx-border-color: #0066cc; " +
                    "-fx-border-radius: 12; " +
                    "-fx-cursor: hand;");
            }
        });
        chip.setOnMouseExited(e -> {
            if (!addedSuggestions.contains(keyword)) {
                chip.setStyle(
                    "-fx-font-size: 11px; " +
                    "-fx-padding: 4 10; " +
                    "-fx-background-color: #e8f4fd; " +
                    "-fx-text-fill: #0066cc; " +
                    "-fx-background-radius: 12; " +
                    "-fx-border-color: #b3d9f2; " +
                    "-fx-border-radius: 12; " +
                    "-fx-cursor: hand;");
            }
        });
        chip.setOnMouseClicked(e -> {
            if (!addedSuggestions.contains(keyword)) {
                keywordsInput.add(keyword);
                addedSuggestions.add(keyword);
                chip.setStyle(
                    "-fx-font-size: 11px; " +
                    "-fx-padding: 4 10; " +
                    "-fx-background-color: #d6d6d6; " +
                    "-fx-text-fill: #888; " +
                    "-fx-background-radius: 12; " +
                    "-fx-border-color: #ccc; " +
                    "-fx-border-radius: 12; " +
                    "-fx-cursor: default;");
            }
        });
        return chip;
    }

    private void updateAiSuggestionVisibility() {
        boolean aiEnabled = SettingsDialog.getBooleanPreference("ai.enabled", true);
        if (!aiEnabled) {
            aiSuggestionBox.setVisible(false);
            aiSuggestionBox.setManaged(false);
        } else {
            aiSuggestionBox.setVisible(true);
            aiSuggestionBox.setManaged(true);

            KeywordSuggestionService service = ServiceFactory.getInstance().createKeywordSuggestionService();
            if (!service.isConfigured()) {
                aiStatusLabel.setText("Configure AI API URL and key in Settings to enable suggestions.");
                aiStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");
            } else {
                aiStatusLabel.setText("");
            }
        }
    }

    private VBox createProjectTagsSection() {
        VBox section = createFieldSection("Project Tags",
            "Select any applicable project affiliations (optional)", false);

        // MenuButton acts as a multi-select dropdown
        projectTagsMenuButton = new MenuButton("Select project tags...");
        projectTagsMenuButton.setStyle(
            "-fx-background-color: white; " +
            "-fx-border-color: #ccc; " +
            "-fx-border-radius: 4; " +
            "-fx-background-radius: 4;");
        projectTagsMenuButton.setPrefWidth(400);

        // Load tags from CV file and add as CheckMenuItems
        List<String> tags = loadProjectTags();
        for (String tag : tags) {
            CheckMenuItem item = new CheckMenuItem(tag);
            item.setOnAction(e -> updateTagChips());
            tagMenuItems.add(item);
        }
        projectTagsMenuButton.getItems().addAll(tagMenuItems);

        // FlowPane to display selected tags as removable chips
        selectedTagsPane = new FlowPane();
        selectedTagsPane.setHgap(6);
        selectedTagsPane.setVgap(4);
        selectedTagsPane.setPadding(new Insets(4, 0, 0, 0));

        section.getChildren().addAll(projectTagsMenuButton, selectedTagsPane);
        return section;
    }

    private List<String> loadProjectTags() {
        List<String> tags = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/cv/projecttag.cv")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            tags.add(line);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not load project tags from CV file", e);
        }
        return tags;
    }

    private void updateTagChips() {
        selectedTagsPane.getChildren().clear();
        for (CheckMenuItem item : tagMenuItems) {
            if (item.isSelected()) {
                Label chip = new Label(item.getText() + "  \u2715");
                chip.setStyle(
                    "-fx-background-color: #0066cc; " +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 3 8; " +
                    "-fx-background-radius: 12; " +
                    "-fx-font-size: 11px; " +
                    "-fx-cursor: hand;");
                chip.setOnMouseClicked(e -> {
                    item.setSelected(false);
                    updateTagChips();
                });
                selectedTagsPane.getChildren().add(chip);
            }
        }
        // Update button text to show count
        long count = tagMenuItems.stream().filter(CheckMenuItem::isSelected).count();
        projectTagsMenuButton.setText(count > 0 ? count + " tag(s) selected" : "Select project tags...");
    }

    private void saveProjectTags() {
        var meta = model.getSubmission().getProjectMetaData();
        if (meta == null) return;
        meta.clearProjectTags();
        List<String> selected = tagMenuItems.stream()
            .filter(CheckMenuItem::isSelected)
            .map(CheckMenuItem::getText)
            .toList();
        if (!selected.isEmpty()) {
            meta.addProjectTags(selected.toArray(new String[0]));
        }
    }

    private VBox createFieldSection(String title, String description, boolean required) {
        VBox section = new VBox(5);

        HBox titleBox = new HBox(5);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        if (required) {
            Label reqLabel = new Label("REQUIRED");
            reqLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: white; -fx-background-color: #dc3545; -fx-padding: 2 4; -fx-background-radius: 4;");
            titleBox.getChildren().addAll(titleLabel, reqLabel);
        } else {
            titleBox.getChildren().add(titleLabel);
        }

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");

        section.getChildren().addAll(titleBox, descLabel);
        return section;
    }

    /**
     * Creates a pane with clickable quick-select buttons for common terms.
     */
    private FlowPane createQuickSelectPane(List<CvParam> terms, OlsAutocomplete targetField) {
        FlowPane pane = new FlowPane();
        pane.setHgap(6);
        pane.setVgap(4);
        pane.setPadding(new Insets(4, 0, 0, 0));

        Label label = new Label("Examples:");
        label.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
        pane.getChildren().add(label);

        for (CvParam term : terms) {
            Label chip = new Label(term.getName());
            chip.setStyle(
                "-fx-font-size: 11px; " +
                "-fx-padding: 2 8; " +
                "-fx-background-color: #e9ecef; " +
                "-fx-background-radius: 10; " +
                "-fx-cursor: hand;");
            chip.setOnMouseEntered(e -> chip.setStyle(
                "-fx-font-size: 11px; " +
                "-fx-padding: 2 8; " +
                "-fx-background-color: #0066cc; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 10; " +
                "-fx-cursor: hand;"));
            chip.setOnMouseExited(e -> chip.setStyle(
                "-fx-font-size: 11px; " +
                "-fx-padding: 2 8; " +
                "-fx-background-color: #e9ecef; " +
                "-fx-background-radius: 10; " +
                "-fx-cursor: hand;"));
            chip.setOnMouseClicked(e -> {
                if (!targetField.getSelectedTerms().contains(term)) {
                    targetField.addTerm(term);
                }
            });
            pane.getChildren().add(chip);
        }

        return pane;
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
        experimentTypeField.getSelectedTerms().addListener((javafx.collections.ListChangeListener.Change<? extends CvParam> c) -> {
            updateValidationFeedback();
            updateCrosslinkBanner();
        });
        softwareField.getSelectedTerms().addListener((javafx.collections.ListChangeListener.Change<? extends CvParam> c) -> updateValidationFeedback());

        // Load existing experiment methods and software from model
        if (!model.getExperimentMethods().isEmpty()) {
            experimentTypeField.setSelectedTerms(model.getExperimentMethods());
        }
        if (!model.getSoftware().isEmpty()) {
            softwareField.setSelectedTerms(model.getSoftware());
        }

        // Validation binding
        valid.bind(Bindings.createBooleanBinding(() -> {
            String title = titleField.getText();
            String desc = descriptionArea.getText();
            String keywords = keywordsInput.getText();
            String sampleProtocol = sampleProtocolArea.getText();
            String dataProtocol = dataProtocolArea.getText();
            boolean hasExperimentType = experimentTypeField.hasSelection();
            boolean hasSoftware = softwareField.hasSelection();

            return isValidTitle(title) &&
                   isValidDescription(desc) &&
                   isNotEmpty(keywords) &&
                   hasExperimentType &&
                   hasSoftware &&
                   isNotEmpty(sampleProtocol) &&
                   isNotEmpty(dataProtocol);
        },
        titleField.textProperty(),
        descriptionArea.textProperty(),
        keywordsInput.textProperty(),
        experimentTypeField.getSelectedTerms(),
        softwareField.getSelectedTerms(),
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

        if (!experimentTypeField.hasSelection()) {
            showError("Please select at least one experiment type");
            experimentTypeField.requestFocus();
            return false;
        }

        if (!softwareField.hasSelection()) {
            showError("Please select at least one software/analysis tool");
            softwareField.requestFocus();
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

        // Check for crosslinking dataset and show banner
        if (isCrosslinkDataset()) {
            crosslinkBanner.setVisible(true);
            crosslinkBanner.setManaged(true);
        }

        // Save project tags and sync to model
        saveProjectTags();
        model.syncMetadataToSubmission();
        return true;
    }

    @Override
    protected void onStepEntering() {
        // Load existing project tags from model
        var meta = model.getSubmission().getProjectMetaData();
        if (meta != null) {
            Set<String> selectedTags = meta.getProjectTags();
            if (selectedTags != null) {
                for (CheckMenuItem item : tagMenuItems) {
                    item.setSelected(selectedTags.contains(item.getText()));
                }
                updateTagChips();
            }
        }

        // Update AI suggestion visibility based on settings
        updateAiSuggestionVisibility();

        // In test mode, pre-fill with example data if fields are empty
        if (model.isTrainingMode()) {
            populateTestModeExamples();
        }
        titleField.requestFocus();
    }

    /**
     * Populate fields with example data for test mode
     */
    private void populateTestModeExamples() {
        // Only populate if fields are empty (don't overwrite user data)
        if (isNullOrEmpty(titleField.getText())) {
            titleField.setText("Quantitative proteomics analysis of human liver cancer cells treated with sorafenib");
        }

        if (isNullOrEmpty(descriptionArea.getText())) {
            descriptionArea.setText(
                "This study investigates the proteomic changes in hepatocellular carcinoma (HCC) cell lines " +
                "following treatment with sorafenib, a multi-kinase inhibitor used in cancer therapy. " +
                "HepG2 cells were cultured and treated with 10 μM sorafenib for 24 hours. " +
                "Control and treated samples were collected in biological triplicates. " +
                "Proteins were extracted, digested with trypsin, and analyzed by LC-MS/MS. " +
                "The data reveals significant changes in signaling pathways related to cell survival and apoptosis."
            );
        }

        if (isNullOrEmpty(keywordsInput.getText())) {
            keywordsInput.setText("liver cancer, hepatocellular carcinoma, sorafenib, proteomics, drug response");
        }

        if (isNullOrEmpty(sampleProtocolArea.getText())) {
            sampleProtocolArea.setText(
                "HepG2 cells were cultured in DMEM supplemented with 10% FBS at 37°C and 5% CO2. " +
                "Cells were treated with 10 μM sorafenib or DMSO vehicle control for 24 hours. " +
                "After treatment, cells were washed with PBS and lysed in 8M urea buffer. " +
                "Proteins were reduced with DTT, alkylated with iodoacetamide, and digested with trypsin overnight. " +
                "Peptides were desalted using C18 spin columns and dried by vacuum centrifugation."
            );
        }

        if (isNullOrEmpty(dataProtocolArea.getText())) {
            dataProtocolArea.setText(
                "Peptides were analyzed on a Q Exactive HF mass spectrometer coupled to an Easy-nLC 1200 system. " +
                "Raw files were processed using MaxQuant (v2.0.3) with the Andromeda search engine. " +
                "Searches were performed against the UniProt human proteome database (UP000005640). " +
                "Label-free quantification (LFQ) was enabled with match between runs. " +
                "Statistical analysis was performed in Perseus using Student's t-test with FDR correction."
            );
        }

        // Add example experiment type if none selected
        if (!experimentTypeField.hasSelection()) {
            List<CvParam> commonTypes = uk.ac.ebi.pride.pxsubmit.service.OlsService.getCommonExperimentTypes();
            if (!commonTypes.isEmpty()) {
                experimentTypeField.addTerm(commonTypes.get(0));
            }
        }

        // Add example software if none selected
        if (!softwareField.hasSelection()) {
            List<CvParam> commonSoftware = uk.ac.ebi.pride.pxsubmit.service.OlsService.getCommonSoftware();
            if (!commonSoftware.isEmpty()) {
                softwareField.addTerm(commonSoftware.get(0));
            }
        }
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
        boolean hasExperimentType = experimentTypeField != null && experimentTypeField.hasSelection();
        boolean hasSoftware = softwareField != null && softwareField.hasSelection();

        int completedFields = 0;
        int totalFields = 7;

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

        // Experiment type validation
        if (!hasExperimentType) {
            validationFeedback.addWarning("Experiment Type: At least one type required");
        } else {
            completedFields++;
        }

        // Software validation
        if (!hasSoftware) {
            validationFeedback.addWarning("Software: At least one analysis tool required");
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
