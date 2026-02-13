package uk.ac.ebi.pride.pxsubmit.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import uk.ac.ebi.pride.pxsubmit.model.SubmissionModel;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Sample Information step - provides SDRF templates for users.
 * This step helps users understand and create SDRF files for their submissions.
 */
public class SampleInformationStep extends AbstractWizardStep {

    private VBox contentBox;
    private Label statusLabel;

    // SDRF template types
    private static final String[][] SDRF_TEMPLATES = {
        {"Default", "default", "Basic SDRF template suitable for most proteomics experiments"},
        {"Cell Line", "cell-line", "Template for cell line-based experiments"},
        {"Human", "human", "Template for human tissue/sample experiments"},
        {"MaxQuant", "maxquant", "Template optimized for MaxQuant search results"},
        {"DIA", "dia", "Template for Data-Independent Acquisition experiments"},
        {"TMT/iTRAQ", "labelled", "Template for labelled quantification experiments (TMT, iTRAQ)"},
        {"Label-Free", "label-free", "Template for label-free quantification experiments"}
    };

    public SampleInformationStep(SubmissionModel model) {
        super("sample-info",
              "Sample Information",
              "Download SDRF templates to describe your samples",
              model);
    }

    @Override
    protected Parent createContent() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        contentBox = new VBox(20);
        contentBox.setPadding(new Insets(30));
        contentBox.setAlignment(Pos.TOP_CENTER);

        // Introduction
        VBox introBox = createIntroSection();
        contentBox.getChildren().add(introBox);

        // Template cards
        VBox templatesBox = createTemplatesSection();
        contentBox.getChildren().add(templatesBox);

        // Status label for feedback
        statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        contentBox.getChildren().add(statusLabel);

        // Info section
        VBox infoBox = createInfoSection();
        contentBox.getChildren().add(infoBox);

        scrollPane.setContent(contentBox);
        return scrollPane;
    }

    private VBox createIntroSection() {
        VBox box = new VBox(10);
        box.setMaxWidth(700);

        Label titleLabel = new Label("Sample-to-Data Relationship File (SDRF)");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label descLabel = new Label(
            "SDRF files describe the relationships between your samples and data files. " +
            "They help ensure your data is properly annotated and can be reused by the scientific community. " +
            "Download a template below that matches your experiment type."
        );
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #555;");

        // Recommendation note
        HBox noteBox = new HBox(8);
        noteBox.setAlignment(Pos.CENTER_LEFT);
        noteBox.setStyle(
            "-fx-background-color: #e7f3ff; " +
            "-fx-padding: 12; " +
            "-fx-background-radius: 6;");

        Label noteIcon = new Label("\u2139");
        noteIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: #0066cc;");

        Label noteText = new Label(
            "Recommended: Include an SDRF file with your submission for better data discovery and reproducibility."
        );
        noteText.setWrapText(true);
        noteText.setStyle("-fx-text-fill: #0066cc;");
        HBox.setHgrow(noteText, Priority.ALWAYS);

        noteBox.getChildren().addAll(noteIcon, noteText);

        box.getChildren().addAll(titleLabel, descLabel, noteBox);
        return box;
    }

    private VBox createTemplatesSection() {
        VBox box = new VBox(12);
        box.setMaxWidth(700);

        Label sectionTitle = new Label("Download SDRF Template");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        box.getChildren().add(sectionTitle);

        // Create template cards
        for (String[] template : SDRF_TEMPLATES) {
            HBox card = createTemplateCard(template[0], template[1], template[2]);
            box.getChildren().add(card);
        }

        return box;
    }

    private HBox createTemplateCard(String name, String type, String description) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12));
        card.setStyle(
            "-fx-background-color: #f8f9fa; " +
            "-fx-border-color: #e9ecef; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6;");

        // Template info
        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        descLabel.setWrapText(true);

        infoBox.getChildren().addAll(nameLabel, descLabel);

        // Download button
        Button downloadBtn = new Button("Download");
        downloadBtn.setStyle(
            "-fx-background-color: #0066cc; " +
            "-fx-text-fill: white; " +
            "-fx-cursor: hand;");
        downloadBtn.setOnAction(e -> downloadTemplate(name, type));

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: #e9ecef; " +
            "-fx-border-color: #dee2e6; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6;"));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: #f8f9fa; " +
            "-fx-border-color: #e9ecef; " +
            "-fx-border-radius: 6; " +
            "-fx-background-radius: 6;"));

        card.getChildren().addAll(infoBox, downloadBtn);
        return card;
    }

    private VBox createInfoSection() {
        VBox box = new VBox(10);
        box.setMaxWidth(700);
        box.setStyle(
            "-fx-background-color: #fff3cd; " +
            "-fx-padding: 15; " +
            "-fx-background-radius: 6;");

        Label titleLabel = new Label("SDRF Format Guide");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #856404;");

        Label guideText = new Label(
            "The SDRF file is a tab-separated file with the following key columns:\n\n" +
            "• source name - Unique identifier for each sample\n" +
            "• characteristics[organism] - Species (e.g., Homo sapiens)\n" +
            "• characteristics[organism part] - Tissue or cell type\n" +
            "• characteristics[disease] - Disease state or 'normal'\n" +
            "• assay name - Name linking to your data files\n" +
            "• comment[data file] - Your RAW file name\n" +
            "• comment[fraction identifier] - Fraction number if applicable\n" +
            "• comment[label] - Label used (e.g., label free, TMT126)\n\n" +
            "For more details, visit the SDRF-Proteomics specification."
        );
        guideText.setWrapText(true);
        guideText.setStyle("-fx-text-fill: #856404;");

        Hyperlink specLink = new Hyperlink("View SDRF-Proteomics Specification");
        specLink.setStyle("-fx-text-fill: #0066cc;");
        specLink.setOnAction(e -> openSdrfSpec());

        box.getChildren().addAll(titleLabel, guideText, specLink);
        return box;
    }

    private void downloadTemplate(String name, String type) {
        String templateContent = generateSdrfTemplate(type);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save SDRF Template");
        fileChooser.setInitialFileName("sdrf-" + type + ".tsv");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("TSV Files", "*.tsv"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showSaveDialog(contentBox.getScene().getWindow());
        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.write(templateContent);
                showStatus("Template saved: " + file.getName(), true);
                logger.info("SDRF template '{}' saved to: {}", name, file.getAbsolutePath());
            } catch (IOException ex) {
                showStatus("Failed to save template: " + ex.getMessage(), false);
                logger.error("Failed to save SDRF template", ex);
            }
        }
    }

    private String generateSdrfTemplate(String type) {
        StringBuilder sb = new StringBuilder();

        // Common headers for all templates
        String[] baseHeaders = {
            "source name",
            "characteristics[organism]",
            "characteristics[organism part]",
            "characteristics[disease]",
            "characteristics[cell type]",
            "assay name",
            "comment[technical replicate]",
            "comment[fraction identifier]",
            "comment[label]",
            "comment[instrument]",
            "comment[data file]"
        };

        switch (type) {
            case "cell-line" -> {
                sb.append(String.join("\t", baseHeaders)).append("\n");
                sb.append("sample1\tHomo sapiens\tnot applicable\tnormal\tHeLa\t")
                  .append("run1\t1\t1\tlabel free\tQ Exactive HF\tfile1.raw\n");
                sb.append("sample2\tHomo sapiens\tnot applicable\tnormal\tHeLa\t")
                  .append("run2\t1\t1\tlabel free\tQ Exactive HF\tfile2.raw\n");
            }
            case "human" -> {
                sb.append(String.join("\t", baseHeaders)).append("\n");
                sb.append("patient1_tumor\tHomo sapiens\tliver\thepatocellular carcinoma\tnot applicable\t")
                  .append("run1\t1\t1\tlabel free\tOrbitrap Fusion\tpatient1_tumor.raw\n");
                sb.append("patient1_normal\tHomo sapiens\tliver\tnormal\tnot applicable\t")
                  .append("run2\t1\t1\tlabel free\tOrbitrap Fusion\tpatient1_normal.raw\n");
            }
            case "maxquant" -> {
                String[] mqHeaders = {
                    "source name",
                    "characteristics[organism]",
                    "characteristics[organism part]",
                    "characteristics[disease]",
                    "assay name",
                    "comment[fraction identifier]",
                    "comment[label]",
                    "comment[instrument]",
                    "comment[data file]",
                    "comment[search engine]"
                };
                sb.append(String.join("\t", mqHeaders)).append("\n");
                sb.append("sample1\tHomo sapiens\tbrain\tnormal\trun1\t")
                  .append("1\tlabel free\tQ Exactive Plus\tsample1.raw\tMaxQuant\n");
            }
            case "dia" -> {
                String[] diaHeaders = {
                    "source name",
                    "characteristics[organism]",
                    "characteristics[organism part]",
                    "characteristics[disease]",
                    "assay name",
                    "comment[fraction identifier]",
                    "comment[label]",
                    "comment[instrument]",
                    "comment[data file]",
                    "comment[acquisition method]"
                };
                sb.append(String.join("\t", diaHeaders)).append("\n");
                sb.append("sample1\tHomo sapiens\tplasma\tnormal\tdia_run1\t")
                  .append("1\tlabel free\tOrbitrap Exploris 480\tsample1.raw\tDIA\n");
            }
            case "labelled" -> {
                String[] tmtHeaders = {
                    "source name",
                    "characteristics[organism]",
                    "characteristics[organism part]",
                    "characteristics[disease]",
                    "assay name",
                    "comment[fraction identifier]",
                    "comment[label]",
                    "comment[instrument]",
                    "comment[data file]"
                };
                sb.append(String.join("\t", tmtHeaders)).append("\n");
                sb.append("control_rep1\tHomo sapiens\tliver\tnormal\ttmt_run1\t1\tTMT126\tOrbitrap Fusion Lumos\ttmt_fraction1.raw\n");
                sb.append("treatment_rep1\tHomo sapiens\tliver\tnormal\ttmt_run1\t1\tTMT127N\tOrbitrap Fusion Lumos\ttmt_fraction1.raw\n");
                sb.append("treatment_rep2\tHomo sapiens\tliver\tnormal\ttmt_run1\t1\tTMT127C\tOrbitrap Fusion Lumos\ttmt_fraction1.raw\n");
            }
            case "label-free" -> {
                sb.append(String.join("\t", baseHeaders)).append("\n");
                sb.append("control_1\tMus musculus\theart\tnormal\tnot applicable\t")
                  .append("lf_run1\t1\t1\tlabel free\tQ Exactive HF-X\tcontrol_1.raw\n");
                sb.append("control_2\tMus musculus\theart\tnormal\tnot applicable\t")
                  .append("lf_run2\t2\t1\tlabel free\tQ Exactive HF-X\tcontrol_2.raw\n");
                sb.append("treatment_1\tMus musculus\theart\tnormal\tnot applicable\t")
                  .append("lf_run3\t1\t1\tlabel free\tQ Exactive HF-X\ttreatment_1.raw\n");
            }
            default -> {
                // Default template
                sb.append(String.join("\t", baseHeaders)).append("\n");
                sb.append("sample1\tHomo sapiens\tnot applicable\tnormal\tnot applicable\t")
                  .append("run1\t1\t1\tlabel free\tQ Exactive\tfile1.raw\n");
                sb.append("sample2\tHomo sapiens\tnot applicable\tnormal\tnot applicable\t")
                  .append("run2\t1\t1\tlabel free\tQ Exactive\tfile2.raw\n");
            }
        }

        return sb.toString();
    }

    private void showStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.setStyle(success ?
            "-fx-text-fill: #28a745; -fx-font-weight: bold;" :
            "-fx-text-fill: #dc3545; -fx-font-weight: bold;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void openSdrfSpec() {
        try {
            java.awt.Desktop.getDesktop().browse(
                new java.net.URI("https://github.com/bigbio/proteomics-sample-metadata"));
        } catch (Exception e) {
            logger.warn("Could not open SDRF specification page", e);
        }
    }

    @Override
    protected void initializeStep() {
        valid.set(true); // This step is always valid (SDRF is recommended but optional)
    }

    @Override
    protected void onStepEntering() {
        // Clear any previous status
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    @Override
    public boolean validate() {
        return true; // Always allow proceeding - SDRF is optional
    }
}
