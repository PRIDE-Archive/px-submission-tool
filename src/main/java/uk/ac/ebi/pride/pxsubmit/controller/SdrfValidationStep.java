package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import uk.ac.ebi.pride.pxsubmit.model.SdrfValidationTracker;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;
import uk.ac.ebi.pride.pxsubmit.service.SdrfValidationOptions;
import uk.ac.ebi.pride.pxsubmit.service.SdrfValidationService;
import uk.ac.ebi.pride.pxsubmit.service.ServiceFactory;
import uk.ac.ebi.pride.pxsubmit.view.component.SearchableMultiSelectField;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Wizard step for validating SDRF files against the PRIDE SDRF Validator API.
 * Skipped automatically when no SDRF files are present.
 */
public class SdrfValidationStep extends AbstractWizardStep {

    private final SdrfValidationService sdrfValidationService =
            ServiceFactory.getInstance().createSdrfValidationService();

    private Label fileLabel;
    private SearchableMultiSelectField templatesField;
    private CheckBox skipOntologyCheck;
    private CheckBox useOlsCacheOnlyCheck;
    private Label statusLabel;
    private Label resultSummary;
    private TextArea issuesArea;
    private Button validateButton;

    public SdrfValidationStep(SubmissionModel model) {
        super("sdrf-validation",
              "SDRF Validation",
              "Validate your sample metadata file(s) against PRIDE SDRF templates",
              model);
    }

    @Override
    public boolean canSkip() {
        return sdrfValidationService.findSdrfFiles(model.getFiles()).isEmpty();
    }

    @Override
    protected Parent createContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox root = new VBox(12);
        root.setPadding(new Insets(15));
        root.setMaxWidth(720);

        Label intro = new Label(
                "Select one or more validator templates and run validation before continuing. "
                        + "Fix any errors in your SDRF file(s) on the previous step if needed.");
        intro.setWrapText(true);
        intro.setStyle("-fx-text-fill: #555;");

        fileLabel = new Label();
        fileLabel.setWrapText(true);

        Label templateHint = new Label(
                "Templates are loaded from the PRIDE SDRF validator API. Combine compatible ones "
                        + "(e.g. ms-proteomics + human + dia-acquisition).");
        templateHint.setWrapText(true);
        templateHint.setStyle("-fx-text-fill: #555;");

        Label sdrfWebHint = new Label(
                "For more detail (all templates, options, and full validation output), use the online tool:");
        sdrfWebHint.setWrapText(true);
        sdrfWebHint.setStyle("-fx-text-fill: #555;");

        Hyperlink sdrfValidatorLink = new Hyperlink("PRIDE SDRF Validator");
        sdrfValidatorLink.setOnAction(e -> openUrl("https://www.ebi.ac.uk/pride/services/sdrf-validator"));
        sdrfValidatorLink.setTooltip(new Tooltip("https://www.ebi.ac.uk/pride/services/sdrf-validator"));

        templatesField = new SearchableMultiSelectField(
                "Search SDRF templates...",
                "No matching templates",
                List.of("ms-proteomics"));
        templatesField.setMaxFieldWidth(480);

        Label templateFieldLabel = new Label("Templates:");
        templateFieldLabel.setStyle("-fx-font-weight: bold;");
        VBox templateSection = new VBox(6, templateFieldLabel, templatesField);

        Label optionsTitle = new Label("Validation options:");
        optionsTitle.setStyle("-fx-font-weight: bold;");

        skipOntologyCheck = new CheckBox("Skip ontology term validation");
        skipOntologyCheck.setSelected(false);
        skipOntologyCheck.setTooltip(new Tooltip(
                "When enabled, ontology terms are not checked (skip_ontology=true). Faster but less strict."));

        useOlsCacheOnlyCheck = new CheckBox("Use only OLS cache for ontology validation");
        useOlsCacheOnlyCheck.setSelected(true);
        useOlsCacheOnlyCheck.setTooltip(new Tooltip(
                "When enabled, ontology lookup uses the local OLS cache only (use_ols_cache_only=true). "
                        + "Faster and works offline; disable for live OLS lookups."));

        skipOntologyCheck.selectedProperty().addListener((obs, was, now) -> {
            if (now) {
                useOlsCacheOnlyCheck.setDisable(true);
            } else {
                useOlsCacheOnlyCheck.setDisable(false);
            }
        });

        VBox optionsBox = new VBox(6, optionsTitle, skipOntologyCheck, useOlsCacheOnlyCheck);

        validateButton = new Button("Validate SDRF");
        validateButton.setDefaultButton(false);
        validateButton.setOnAction(e -> runValidation());

        statusLabel = new Label("Choose template(s) and click Validate SDRF.");
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: #555;");

        resultSummary = new Label("No validation result yet.");
        resultSummary.setWrapText(true);
        resultSummary.setStyle("-fx-text-fill: #555;");

        Label issuesTitle = new Label("Errors and warnings:");
        issuesTitle.setStyle("-fx-font-weight: bold;");

        issuesArea = new TextArea();
        issuesArea.setEditable(false);
        issuesArea.setWrapText(true);
        issuesArea.setPrefRowCount(14);
        issuesArea.setPromptText("No errors or warnings");
        issuesArea.setStyle("-fx-control-inner-background: #fafafa;");

        root.getChildren().addAll(
                intro,
                fileLabel,
                templateHint,
                sdrfWebHint,
                sdrfValidatorLink,
                templateSection,
                optionsBox,
                validateButton,
                statusLabel,
                resultSummary,
                issuesTitle,
                issuesArea
        );

        scrollPane.setContent(root);
        return scrollPane;
    }

    @Override
    protected void onStepEntering() {
        List<File> sdrfFiles = sdrfValidationService.findSdrfFiles(model.getFiles());
        SdrfValidationTracker tracker = model.getSdrfValidation();
        tracker.syncFileSet(sdrfFiles);

        fileLabel.setText("SDRF file(s): " +
                sdrfFiles.stream().map(File::getName).collect(Collectors.joining(", ")));

        refreshValidFromTracker(sdrfFiles);
        loadTemplateMenuAsync();
        notifyFileSelectionStep();
    }

    @Override
    public boolean validate() {
        List<File> sdrfFiles = sdrfValidationService.findSdrfFiles(model.getFiles());
        SdrfValidationTracker tracker = model.getSdrfValidation();
        tracker.syncFileSet(sdrfFiles);

        if (tracker.isValidatedForCurrentFiles(sdrfFiles)) {
            return true;
        }
        statusLabel.setText("Run validation and resolve all errors before continuing.");
        statusLabel.setStyle("-fx-text-fill: #dc3545;");
        return false;
    }

    private void runValidation() {
        List<File> sdrfFiles = sdrfValidationService.findSdrfFiles(model.getFiles());
        if (sdrfFiles.isEmpty()) {
            return;
        }

        List<String> selectedTemplates = templatesField.getSelectedItems();
        if (selectedTemplates.isEmpty()) {
            statusLabel.setText("Please select at least one template.");
            statusLabel.setStyle("-fx-text-fill: #dc3545;");
            setValid(false);
            return;
        }

        statusLabel.setText("Validating SDRF file(s) with: " + String.join(", ", selectedTemplates) + "...");
        statusLabel.setStyle("-fx-text-fill: #555;");
        validateButton.setDisable(true);
        if (wizardController != null) {
            wizardController.showGlobalProgress();
        }
        issuesArea.clear();
        resultSummary.setText("Validation in progress...");
        resultSummary.setStyle("-fx-text-fill: #555;");
        setValid(false);

        SdrfValidationOptions validationOptions = new SdrfValidationOptions(
                List.copyOf(selectedTemplates),
                skipOntologyCheck.isSelected(),
                useOlsCacheOnlyCheck.isSelected()
        );
        final String signature = SdrfValidationTracker.buildSignature(sdrfFiles);
        final List<String> templatesToValidate = selectedTemplates;

        Thread.startVirtualThread(() -> {
            SdrfValidationService.BatchResult batchResult =
                    sdrfValidationService.validateBatch(sdrfFiles, validationOptions);
            Platform.runLater(() -> {
                SdrfValidationTracker tracker = model.getSdrfValidation();
                tracker.applyResults(signature, batchResult.resultsByPath(), batchResult.allPassed());
                refreshValidationUi(batchResult, templatesToValidate, validationOptions);
                refreshValidFromTracker(sdrfFiles);
                validateButton.setDisable(false);
                if (wizardController != null) {
                    wizardController.hideGlobalProgress();
                }
                notifyFileSelectionStep();
            });
        });
    }

    private void refreshValidationUi(
            SdrfValidationService.BatchResult batchResult,
            List<String> templatesToValidate,
            SdrfValidationOptions validationOptions) {
        List<String> allErrors = batchResult.errors();
        List<String> allWarnings = batchResult.warnings();
        boolean finalAllValid = batchResult.allPassed();

        String optionsSummary = validationOptions.skipOntology()
                ? ", skip ontology"
                : (validationOptions.useOlsCacheOnly() ? ", OLS cache only" : ", live OLS");
        statusLabel.setText("Validation completed (" + templatesToValidate.size() + " template"
                + (templatesToValidate.size() == 1 ? "" : "s") + ": "
                + String.join(", ", templatesToValidate) + optionsSummary + ").");
        statusLabel.setStyle("-fx-text-fill: #555;");

        issuesArea.setText(SdrfValidationService.formatValidationIssues(allErrors, allWarnings));

        if (finalAllValid && allErrors.isEmpty() && allWarnings.isEmpty()) {
            resultSummary.setText("SDRF validation passed. No errors or warnings.");
            resultSummary.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
        } else if (allErrors.isEmpty()) {
            resultSummary.setText("SDRF validation passed with warnings.");
            resultSummary.setStyle("-fx-text-fill: #856404; -fx-font-weight: bold;");
        } else {
            resultSummary.setText("SDRF validation failed. Please review issues below.");
            resultSummary.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
        }
    }

    private void refreshValidFromTracker(List<File> sdrfFiles) {
        setValid(model.getSdrfValidation().isValidatedForCurrentFiles(sdrfFiles));
    }

    private void loadTemplateMenuAsync() {
        Thread.startVirtualThread(() -> {
            List<String> templateNames;
            try {
                templateNames = sdrfValidationService.loadTemplatesAsync().get(30, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.warn("Timed out loading SDRF templates from API");
                templateNames = List.of();
            } catch (Exception e) {
                logger.warn("Failed waiting for SDRF templates", e);
                templateNames = List.of();
            }
            List<String> selectable = sdrfValidationService.resolveSelectableTemplates(templateNames);
            Platform.runLater(() -> templatesField.setAllItems(selectable));
        });
    }

    private void notifyFileSelectionStep() {
        if (wizardController == null) {
            return;
        }
        wizardController.getStep("file-selection").ifPresent(step -> {
            if (step instanceof FileSelectionStep fileStep) {
                fileStep.refreshSdrfValidationDisplay();
            }
        });
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            logger.warn("Could not open URL: {}", url, e);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Open in browser");
            alert.setHeaderText(null);
            alert.setContentText("Please open this link in your browser:\n" + url);
            alert.showAndWait();
        }
    }
}
