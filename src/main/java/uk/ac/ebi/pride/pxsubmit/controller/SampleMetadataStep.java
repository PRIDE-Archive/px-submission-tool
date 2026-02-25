package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.service.OlsService;
import uk.ac.ebi.pride.pxsubmit.service.OlsService.OlsOntology;
import uk.ac.ebi.pride.pxsubmit.service.SdrfParserService;
import uk.ac.ebi.pride.pxsubmit.service.ServiceFactory;
import uk.ac.ebi.pride.pxsubmit.service.SdrfParserService.SdrfData;
import uk.ac.ebi.pride.pxsubmit.view.component.OlsAutocomplete;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Step for entering sample-level metadata.
 *
 * Features:
 * - Automatic SDRF detection and parsing
 * - OLS autocomplete for ontology terms
 * - Common terms quick selection
 * - Support for: Organism, Tissue, Cell Type, Disease, Instrument, Modifications
 */
public class SampleMetadataStep extends AbstractWizardStep {

    // SDRF detection
    private VBox sdrfInfoBox;
    private Label sdrfStatusLabel;
    private SdrfData parsedSdrfData;

    // Metadata fields
    private OlsAutocomplete speciesField;
    private OlsAutocomplete tissueField;
    private OlsAutocomplete cellTypeField;
    private OlsAutocomplete diseaseField;
    private OlsAutocomplete instrumentField;
    private OlsAutocomplete modificationField;

    // Status
    private Label validationLabel;

    public SampleMetadataStep(SubmissionModel model) {
        super("sample-metadata",
              "Sample Metadata",
              "Enter information about your samples and experimental setup",
              model);
    }

    @Override
    public boolean canSkip() {
        // Skip this step during resubmission - sample metadata already exists on the server
        return model.isResubmissionMode();
    }

    @Override
    protected Parent createContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // SDRF Detection Info Box
        sdrfInfoBox = createSdrfInfoBox();

        // Metadata form
        VBox formBox = new VBox(15);

        // Species (Mandatory)
        VBox speciesSection = createFieldSection(
                "Species / Organism *",
                "Search for the organism(s) used in your study",
                true);
        speciesField = new OlsAutocomplete(OlsOntology.NCBI_TAXON);
        speciesField.setPromptText("Search species (e.g., Homo sapiens, mouse)...");
        speciesField.setCommonTerms(OlsService.getCommonSpecies());
        speciesField.setOnTermSelected(term -> model.addSpecies(term));
        speciesField.setOnTermRemoved(term -> model.removeSpecies(term));
        speciesSection.getChildren().add(speciesField);
        speciesSection.getChildren().add(createQuickSelectPane(OlsService.getCommonSpecies(), speciesField));

        // Tissue (Mandatory)
        VBox tissueSection = createFieldSection(
                "Tissue / Organism Part *",
                "Search for the tissue(s) or body part(s) analyzed. You can add more after SDRF loading.",
                true);
        tissueField = new OlsAutocomplete(OlsOntology.BTO);
        tissueField.setPromptText("Search tissue (e.g., liver, blood, brain)...");
        tissueField.setCommonTerms(OlsService.getCommonTissues());
        tissueField.setOnTermSelected(term -> model.addTissue(term));
        tissueField.setOnTermRemoved(term -> model.removeTissue(term));
        tissueSection.getChildren().add(tissueField);
        tissueSection.getChildren().add(createQuickSelectPane(OlsService.getCommonTissues(), tissueField));

        // Cell Type (Mandatory)
        VBox cellTypeSection = createFieldSection(
                "Cell Type / Cell Line *",
                "Search for cell type or cell line if applicable. You can add more after SDRF loading.",
                true);
        cellTypeField = new OlsAutocomplete(OlsOntology.CL);
        cellTypeField.setPromptText("Search cell type (e.g., T cell, HeLa)...");
        cellTypeField.setCommonTerms(OlsService.getCommonCellTypes());
        cellTypeField.setOnTermSelected(term -> model.addCellType(term));
        cellTypeField.setOnTermRemoved(term -> model.removeCellType(term));
        cellTypeSection.getChildren().add(cellTypeField);
        cellTypeSection.getChildren().add(createQuickSelectPane(OlsService.getCommonCellTypes(), cellTypeField));

        // Disease (Mandatory)
        VBox diseaseSection = createFieldSection(
                "Disease *",
                "Search for disease if studying a pathological condition. You can add more after SDRF loading.",
                true);
        diseaseField = new OlsAutocomplete(OlsOntology.DOID);
        diseaseField.setPromptText("Search disease (e.g., cancer, diabetes)...");
        diseaseField.setCommonTerms(OlsService.getCommonDiseases());
        diseaseField.setOnTermSelected(term -> model.addDisease(term));
        diseaseField.setOnTermRemoved(term -> model.removeDisease(term));
        diseaseSection.getChildren().add(diseaseField);
        diseaseSection.getChildren().add(createQuickSelectPane(OlsService.getCommonDiseases(), diseaseField));

        // Instrument (Mandatory)
        VBox instrumentSection = createFieldSection(
                "Instrument *",
                "Search for the mass spectrometer(s) used",
                true);
        instrumentField = new OlsAutocomplete(OlsOntology.MS);
        instrumentField.setPromptText("Search instrument (e.g., Q Exactive, Orbitrap)...");
        instrumentField.setCommonTerms(OlsService.getCommonInstruments());
        instrumentField.setOnTermSelected(term -> model.addInstrument(term));
        instrumentField.setOnTermRemoved(term -> model.removeInstrument(term));
        instrumentSection.getChildren().add(instrumentField);
        instrumentSection.getChildren().add(createQuickSelectPane(OlsService.getCommonInstruments(), instrumentField));

        // Modifications (Mandatory)
        VBox modificationSection = createFieldSection(
                "Modifications *",
                "Search for post-translational modifications studied",
                true);
        modificationField = new OlsAutocomplete(OlsOntology.MOD);
        modificationField.setPromptText("Search modification (e.g., phosphorylation, acetylation)...");
        modificationField.setCommonTerms(OlsService.getCommonModifications());
        modificationField.setOnTermSelected(term -> model.addModification(term));
        modificationField.setOnTermRemoved(term -> model.removeModification(term));
        modificationSection.getChildren().add(modificationField);
        modificationSection.getChildren().add(createQuickSelectPane(OlsService.getCommonModifications(), modificationField));

        // Validation status
        validationLabel = new Label();
        validationLabel.setStyle("-fx-font-weight: bold;");

        formBox.getChildren().addAll(
                speciesSection,
                tissueSection,
                cellTypeSection,
                diseaseSection,
                new Separator(),
                instrumentSection,
                modificationSection,
                new Separator(),
                validationLabel
        );

        content.getChildren().addAll(sdrfInfoBox, formBox);
        scrollPane.setContent(content);
        return scrollPane;
    }

    private VBox createSdrfInfoBox() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: #e7f3ff; -fx-border-color: #0066cc; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label titleLabel = new Label("SDRF File Detection");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0066cc;");

        sdrfStatusLabel = new Label("Checking for SDRF file...");
        sdrfStatusLabel.setStyle("-fx-text-fill: #666;");

        Label helpLabel = new Label(
                "If you included an SDRF file, metadata will be automatically extracted. " +
                "You can also enter metadata manually below.");
        helpLabel.setWrapText(true);
        helpLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        box.getChildren().addAll(titleLabel, sdrfStatusLabel, helpLabel);
        return box;
    }

    private VBox createFieldSection(String title, String description, boolean required) {
        VBox section = new VBox(5);

        HBox titleBox = new HBox(5);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        if (required) {
            Label reqLabel = new Label("REQUIRED");
            reqLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: white; -fx-background-color: #dc3545; -fx-padding: 2 4; -fx-background-radius: 4;");
            titleBox.getChildren().addAll(titleLabel, reqLabel);
        } else {
            titleBox.getChildren().add(titleLabel);
        }

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

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
        // Validation: all fields are mandatory
        valid.bind(Bindings.createBooleanBinding(() ->
            speciesField.hasSelection() &&
            tissueField.hasSelection() &&
            cellTypeField.hasSelection() &&
            diseaseField.hasSelection() &&
            instrumentField.hasSelection() &&
            modificationField.hasSelection(),
            speciesField.getSelectedTerms(),
            tissueField.getSelectedTerms(),
            cellTypeField.getSelectedTerms(),
            diseaseField.getSelectedTerms(),
            instrumentField.getSelectedTerms(),
            modificationField.getSelectedTerms()
        ));

        // Update validation label
        valid.addListener((obs, oldVal, newVal) -> updateValidationLabel());
    }

    @Override
    protected void onStepEntering() {
        // Check for SDRF file
        checkForSdrfFile();

        // Load existing metadata from model
        loadFromModel();

        // Update validation
        updateValidationLabel();
    }

    @Override
    protected void onStepLeaving() {
        // Save to model is done via callbacks, but ensure sync
        syncToModel();
    }

    private void checkForSdrfFile() {
        // Find ALL SDRF files in the file list
        List<File> sdrfFiles = model.getFiles().stream()
                .filter(df -> df.getFileType() == ProjectFileType.EXPERIMENTAL_DESIGN ||
                             SdrfParserService.isSdrfFile(df.getFile()))
                .map(DataFile::getFile)
                .toList();

        if (sdrfFiles.isEmpty()) {
            sdrfStatusLabel.setText("No SDRF file found - please enter metadata manually below");
            sdrfInfoBox.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #ffc107; -fx-border-radius: 4; -fx-background-radius: 4;");
            return;
        }

        String fileNames = sdrfFiles.stream().map(File::getName).reduce((a, b) -> a + ", " + b).orElse("");
        sdrfStatusLabel.setText("Found " + sdrfFiles.size() + " SDRF file(s): " + fileNames + " - Parsing...");
        sdrfInfoBox.setStyle("-fx-background-color: #d4edda; -fx-border-color: #28a745; -fx-border-radius: 4; -fx-background-radius: 4;");

        // Merged metadata from all SDRF files
        Set<CvParam> allOrganisms = new LinkedHashSet<>();
        Set<CvParam> allTissues = new LinkedHashSet<>();
        Set<CvParam> allCellTypes = new LinkedHashSet<>();
        Set<CvParam> allDiseases = new LinkedHashSet<>();
        Set<CvParam> allInstruments = new LinkedHashSet<>();
        Set<CvParam> allModifications = new LinkedHashSet<>();
        Set<CvParam> allLabels = new LinkedHashSet<>();
        AtomicInteger totalSamples = new AtomicInteger(0);
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        for (File file : sdrfFiles) {
            SdrfParserService parser = ServiceFactory.getInstance().createSdrfParserService(file);
            parser.setOnSucceeded(e -> {
                SdrfData data = parser.getValue();
                if (data != null && !data.isEmpty()) {
                    synchronized (allOrganisms) {
                        allOrganisms.addAll(data.organisms());
                        allTissues.addAll(data.tissues());
                        allCellTypes.addAll(data.cellTypes());
                        allDiseases.addAll(data.diseases());
                        allInstruments.addAll(data.instruments());
                        allModifications.addAll(data.modifications());
                        allLabels.addAll(data.labels());
                        totalSamples.addAndGet(data.getSampleCount());
                    }
                }
                if (completedCount.incrementAndGet() + failedCount.get() == sdrfFiles.size()) {
                    Platform.runLater(() -> finalizeSdrfMerge(
                        sdrfFiles.size(), totalSamples.get(), failedCount.get(),
                        allOrganisms, allTissues, allCellTypes, allDiseases,
                        allInstruments, allModifications, allLabels));
                }
            });
            parser.setOnFailed(e -> {
                logger.warn("Failed to parse SDRF: {}", file.getName(), e.getSource().getException());
                if (failedCount.incrementAndGet() + completedCount.get() == sdrfFiles.size()) {
                    Platform.runLater(() -> finalizeSdrfMerge(
                        sdrfFiles.size(), totalSamples.get(), failedCount.get(),
                        allOrganisms, allTissues, allCellTypes, allDiseases,
                        allInstruments, allModifications, allLabels));
                }
            });
            parser.start();
        }
    }

    private void finalizeSdrfMerge(int totalFiles, int totalSamples, int failedFiles,
                                    Set<CvParam> organisms, Set<CvParam> tissues,
                                    Set<CvParam> cellTypes, Set<CvParam> diseases,
                                    Set<CvParam> instruments, Set<CvParam> modifications,
                                    Set<CvParam> labels) {
        // Build merged SdrfData for storage
        parsedSdrfData = new SdrfData(
            Collections.emptyList(), organisms, tissues, cellTypes, diseases,
            instruments, modifications, labels,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());

        String status = String.format(
            "Merged from %d SDRF file(s): %d samples, %d organisms, %d tissues, %d instruments",
            totalFiles - failedFiles, totalSamples,
            organisms.size(), tissues.size(), instruments.size());
        if (failedFiles > 0) {
            status += " (" + failedFiles + " file(s) failed to parse)";
        }
        sdrfStatusLabel.setText(status);

        if (failedFiles > 0 && failedFiles < totalFiles) {
            sdrfInfoBox.setStyle("-fx-background-color: #fff3cd; -fx-border-color: #ffc107; -fx-border-radius: 4; -fx-background-radius: 4;");
        } else if (failedFiles == totalFiles) {
            sdrfInfoBox.setStyle("-fx-background-color: #f8d7da; -fx-border-color: #dc3545; -fx-border-radius: 4; -fx-background-radius: 4;");
            return;
        }

        populateFromSdrf(parsedSdrfData);
    }

    private void populateFromSdrf(SdrfData data) {
        // Populate species
        if (data.hasOrganism()) {
            speciesField.setSelectedTerms(data.organisms().stream().toList());
            for (CvParam org : data.organisms()) {
                model.addSpecies(org);
            }
        }

        // Populate tissues
        if (data.hasTissue()) {
            tissueField.setSelectedTerms(data.tissues().stream().toList());
        }

        // Populate cell types
        if (!data.cellTypes().isEmpty()) {
            cellTypeField.setSelectedTerms(data.cellTypes().stream().toList());
        }

        // Populate diseases
        if (!data.diseases().isEmpty()) {
            diseaseField.setSelectedTerms(data.diseases().stream().toList());
        }

        // Populate instruments
        if (data.hasInstrument()) {
            instrumentField.setSelectedTerms(data.instruments().stream().toList());
            for (CvParam inst : data.instruments()) {
                model.addInstrument(inst);
            }
        }

        // Populate modifications
        if (!data.modifications().isEmpty()) {
            modificationField.setSelectedTerms(data.modifications().stream().toList());
            for (CvParam mod : data.modifications()) {
                model.addModification(mod);
            }
        }

        updateValidationLabel();
    }

    private void loadFromModel() {
        // Load existing selections from model
        if (!model.getSpecies().isEmpty()) {
            speciesField.setSelectedTerms(model.getSpecies());
        }
        if (!model.getInstruments().isEmpty()) {
            instrumentField.setSelectedTerms(model.getInstruments());
        }
        if (!model.getModifications().isEmpty()) {
            modificationField.setSelectedTerms(model.getModifications());
        }
    }

    private void syncToModel() {
        // Species and instruments are synced via callbacks
        // Ensure other fields are saved if needed
        model.syncMetadataToSubmission();
    }

    private void updateValidationLabel() {
        boolean hasSpecies = speciesField.hasSelection();
        boolean hasTissue = tissueField.hasSelection();
        boolean hasCellType = cellTypeField.hasSelection();
        boolean hasDisease = diseaseField.hasSelection();
        boolean hasInstrument = instrumentField.hasSelection();
        boolean hasModification = modificationField.hasSelection();

        if (hasSpecies && hasTissue && hasCellType && hasDisease && hasInstrument && hasModification) {
            validationLabel.setText("\u2714 All required fields completed");
            validationLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #28a745;");
        } else {
            StringBuilder missing = new StringBuilder("Missing: ");
            if (!hasSpecies) missing.append("Species, ");
            if (!hasTissue) missing.append("Tissue, ");
            if (!hasCellType) missing.append("Cell Type, ");
            if (!hasDisease) missing.append("Disease, ");
            if (!hasInstrument) missing.append("Instrument, ");
            if (!hasModification) missing.append("Modifications, ");
            // Remove trailing comma and space
            String missingStr = missing.substring(0, missing.length() - 2);
            validationLabel.setText("\u26A0 " + missingStr);
            validationLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc3545;");
        }
    }

    @Override
    public boolean validate() {
        if (!speciesField.hasSelection()) {
            showError("Please select at least one species/organism");
            speciesField.requestFocus();
            return false;
        }

        if (!tissueField.hasSelection()) {
            showError("Please select at least one tissue/organism part");
            tissueField.requestFocus();
            return false;
        }

        if (!cellTypeField.hasSelection()) {
            showError("Please select at least one cell type/cell line");
            cellTypeField.requestFocus();
            return false;
        }

        if (!diseaseField.hasSelection()) {
            showError("Please select at least one disease (use 'healthy' if not applicable)");
            diseaseField.requestFocus();
            return false;
        }

        if (!instrumentField.hasSelection()) {
            showError("Please select at least one mass spectrometry instrument");
            instrumentField.requestFocus();
            return false;
        }

        if (!modificationField.hasSelection()) {
            showError("Please select at least one modification");
            modificationField.requestFocus();
            return false;
        }

        syncToModel();
        return true;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Missing Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
