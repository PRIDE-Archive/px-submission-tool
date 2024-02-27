package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.util.Constant;
import uk.ac.ebi.pride.data.validation.SubmissionValidator;
import uk.ac.ebi.pride.gui.form.combo.model.ResultFileSelectionModel;
import uk.ac.ebi.pride.gui.form.combo.renderer.ResultFileMetaDataComboRenderer;
import uk.ac.ebi.pride.gui.form.comp.HintedTextArea;
import uk.ac.ebi.pride.gui.form.table.TableFactory;
import uk.ac.ebi.pride.gui.form.table.model.MetaDataTableModel;
import uk.ac.ebi.pride.gui.util.BalloonTipUtil;
import uk.ac.ebi.pride.gui.util.BorderUtil;
import uk.ac.ebi.pride.gui.util.ValidationState;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Set;

/**
 * Form to capture additional metadata about the submission, such as: species, instrument and modification
 * <p/>
 * Note: this form will only appear if unsupported result files are detected
 *
 * @author Rui Wang
 * @version $Id$
 */
public class AdditionalMetaDataForm extends Form {

    private static final float DEFAULT_TITLE_FONT_SIZE = 15f;

    private JPanel commentInputPanel;

    private JTextArea commentTextArea;

    private JPanel speciesPanel;
    private JPanel tissuePanel;
    private JPanel cellTypePanel;
    private JPanel diseasePanel;
    private JPanel modificationPanel;
    private JPanel instrumentPanel;
    private JPanel quantPanel;
    private MetaDataTableModel speciesTableModel;
    private MetaDataTableModel tissueTableModel;
    private MetaDataTableModel cellTypeTableModel;
    private MetaDataTableModel diseaseTableModel;
    private MetaDataTableModel instrumentTableModel;
    private MetaDataTableModel quantTableModel;
    private MetaDataTableModel modificationTableModel;

    public AdditionalMetaDataForm() {
        initComponents();
    }

    private void initComponents() {
        // setup the main pane
        GridLayout gridLayout = new GridLayout(4, 2);
        this.setLayout(gridLayout);

        // setup submission summary description panel
        // species
        speciesTableModel = new MetaDataTableModel();
        speciesPanel = initMetadataPanel(appContext.getProperty("species.label.title"),
                true,
                appContext.getProperty("species.combobox.default.selection"),
                appContext.getProperty("species.combobox.other.species"),
                appContext.getProperty("species.combobox.default.species.file"),
                Constant.NEWT,
                speciesTableModel);
        this.add(speciesPanel);

        // tissue
        tissueTableModel = new MetaDataTableModel();
        tissuePanel = initMetadataPanel(appContext.getProperty("tissue.label.title"),
                true,
                appContext.getProperty("tissue.combobox.default.selection"),
                appContext.getProperty("tissue.combobox.other.tissue"),
                appContext.getProperty("tissue.combobox.default.tissue.file"),
                Constant.BTO,
                tissueTableModel);
        this.add(tissuePanel);

        // modification
        modificationTableModel = new MetaDataTableModel();
        modificationPanel = initMetadataPanel(appContext.getProperty("modification.label.title"),
                true,
                appContext.getProperty("modification.combobox.default.selection"),
                appContext.getProperty("modification.combobox.other.modification"),
                appContext.getProperty("modification.combobox.default.modification.file"),
                Constant.PSI_MOD,
                modificationTableModel);
        this.add(modificationPanel);

        // instrument
        instrumentTableModel = new MetaDataTableModel();
        instrumentPanel = initMetadataPanel(appContext.getProperty("instrument.label.title"),
                true,
                appContext.getProperty("instrument.combobox.default.selection"),
                appContext.getProperty("instrument.combobox.other.instrument"),
                appContext.getProperty("instrument.combobox.default.instrument.file"),
                Constant.MS,
                instrumentTableModel);
        this.add(instrumentPanel);

        // cell type
        cellTypeTableModel = new MetaDataTableModel();
        cellTypePanel = initMetadataPanel(appContext.getProperty("celltype.label.title"),
                false,
                appContext.getProperty("celltype.combobox.default.selection"),
                appContext.getProperty("celltype.combobox.other.celltype"),
                appContext.getProperty("celltype.combobox.default.celltype.file"),
                Constant.CL,
                cellTypeTableModel);
        this.add(cellTypePanel);

        // disease
        diseaseTableModel = new MetaDataTableModel();
        diseasePanel = initMetadataPanel(appContext.getProperty("disease.label.title"),
                false,
                appContext.getProperty("disease.combobox.default.selection"),
                appContext.getProperty("disease.combobox.other.disease"),
                appContext.getProperty("disease.combobox.default.disease.file"),
                Constant.DOID,
                diseaseTableModel);
        this.add(diseasePanel);

        // quantification
        quantTableModel = new MetaDataTableModel();
        quantPanel = initMetadataPanel(appContext.getProperty("quantification.label.title"),
                false,
                appContext.getProperty("quantification.combobox.default.selection"),
                appContext.getProperty("quantification.combobox.other.quantification"),
                appContext.getProperty("quantification.combobox.default.quantification.file"),
                Constant.PRIDE,
                quantTableModel);
        this.add(quantPanel);

        // setup submission summary table
//        initCommentPanel();
    }


    public JPanel getInstrumentPanel() {
        return instrumentPanel;
    }

    public JPanel getSpeciesPanel() {
        return speciesPanel;
    }

    public JPanel getQuantPanel() {
        return quantPanel;
    }

    public void setInstrumentPanel(JPanel instrumentPanel) {
        this.instrumentPanel = instrumentPanel;
    }

    public void setSpeciesPanel(JPanel speciesPanel) {
        this.speciesPanel = speciesPanel;
    }

    public void setQuantPanel(JPanel quantPanel) {
        this.quantPanel = quantPanel;
    }

    public void setInstrumentTableModel(MetaDataTableModel instrumentTableModel) {
        this.instrumentTableModel = instrumentTableModel;
    }

    public void setSpeciesTableModel(MetaDataTableModel speciesTableModel) {
        this.speciesTableModel = speciesTableModel;
    }

    public void setQuantTableModel(MetaDataTableModel quantTableModel) {
        this.quantTableModel = quantTableModel;
    }

    /**
     * Initialize metadata details panel
     */
    public static JPanel initMetadataPanel(String panelTitle,
                                     boolean required,
                                     String defaultSelectionLabel,
                                     String otherSelectionLabel,
                                     String defaultValueFile,
                                     String ontology,
                                     MetaDataTableModel tableModel) {
        JPanel metaDataPanel = new JPanel(new BorderLayout());

        // metadata label
        JPanel titlePanel = new JPanel(new BorderLayout());
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        labelPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 2, 0));

        JLabel metaDataPanelLabel = new JLabel(panelTitle + (required ? "*" : ""));
        metaDataPanelLabel.setFont(metaDataPanelLabel.getFont().deriveFont(DEFAULT_TITLE_FONT_SIZE).deriveFont(required ? Font.BOLD : Font.PLAIN));
        labelPanel.add(metaDataPanelLabel);
        titlePanel.add(labelPanel, BorderLayout.WEST);

        metaDataPanel.add(titlePanel, BorderLayout.NORTH);

        // metadata selection panel
        JPanel metaDataSelectionPanel = new JPanel(new BorderLayout());
        metaDataSelectionPanel.setBorder(BorderUtil.createLoweredBorder());

        JComboBox metaDataSelectionComboBox = new JComboBox();
        metaDataSelectionComboBox.setFont(metaDataSelectionComboBox.getFont().deriveFont(14f));
        metaDataSelectionComboBox.setModel(new ResultFileSelectionModel(defaultSelectionLabel, otherSelectionLabel, defaultValueFile, ontology, tableModel));
        metaDataSelectionComboBox.setRenderer(new ResultFileMetaDataComboRenderer(tableModel));
        metaDataSelectionPanel.add(metaDataSelectionComboBox, BorderLayout.NORTH);

        JTable metaDataTable = TableFactory.createMetadataTable(tableModel);

        JScrollPane scrollPane = new JScrollPane(metaDataTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        metaDataSelectionPanel.add(scrollPane, BorderLayout.CENTER);
        metaDataPanel.add(metaDataSelectionPanel, BorderLayout.CENTER);

        return metaDataPanel;
    }

    /**
     * Initialize submission summary table
     */
    private void initCommentPanel() {
        JPanel commentPanel = new JPanel(new BorderLayout());

        // modification label
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        labelPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 2, 0));

        JLabel modLabel = new JLabel(appContext.getProperty("comment.label.title"));
        modLabel.setFont(modLabel.getFont().deriveFont(DEFAULT_TITLE_FONT_SIZE));
        labelPanel.add(modLabel);
        JLabel requiredLabel = new JLabel(appContext.getProperty("required.label.title"));
        requiredLabel.setForeground(Color.gray);
        labelPanel.add(requiredLabel);
        commentPanel.add(labelPanel, BorderLayout.NORTH);

        commentInputPanel = new JPanel(new BorderLayout());
        commentInputPanel.setBorder(BorderUtil.createLoweredBorder());

        commentTextArea = new HintedTextArea(appContext.getProperty("comment.default.text"));
        commentTextArea.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(commentTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        commentInputPanel.add(scrollPane, BorderLayout.CENTER);
        commentPanel.add(commentInputPanel, BorderLayout.CENTER);

        this.add(commentPanel);
    }

    public Set<CvParam> getSpecies() {
        return speciesTableModel.getValues();
    }

    public void setSpecies(Collection<CvParam> species) {
        speciesTableModel.clearValues();
        speciesTableModel.addValues(species);
    }

    public Set<CvParam> getTissues() {
        return tissueTableModel.getValues();
    }

    public void setTissues(Collection<CvParam> tissues) {
        tissueTableModel.clearValues();
        tissueTableModel.addValues(tissues);
    }

    public Set<CvParam> getCellTypes() {
        return cellTypeTableModel.getValues();
    }

    public void setCellTypes(Collection<CvParam> cellTypes) {
        cellTypeTableModel.clearValues();
        cellTypeTableModel.addValues(cellTypes);
    }

    public Set<CvParam> getDiseases() {
        return diseaseTableModel.getValues();
    }

    public void setDiseases(Collection<CvParam> diseases) {
        diseaseTableModel.clearValues();
        diseaseTableModel.addValues(diseases);
    }

    public Set<CvParam> getInstruments() {
        return instrumentTableModel.getValues();
    }

    public void setInstruments(Collection<CvParam> instruments) {
        instrumentTableModel.clearValues();
        instrumentTableModel.addValues(instruments);
    }

    public Set<CvParam> getModifications() {
        return modificationTableModel.getValues();
    }

    public void setModifications(Collection<CvParam> modifications) {
        modificationTableModel.clearValues();
        modificationTableModel.addValues(modifications);
    }

    public Set<CvParam> getQuantifications() {
        return quantTableModel.getValues();
    }

    public void setQuantifications(Collection<CvParam> quants) {
        quantTableModel.clearValues();
        quantTableModel.addValues(quants);
    }

    public void setComment(String comment) {
        commentTextArea.setText(comment);
    }

    public String getComment() {
        return commentTextArea.getText().trim();
    }

    public ValidationState doValidation() {
        boolean invalid = false;

        if (warningBalloonTip != null) {
            warningBalloonTip.closeBalloon();
        }

        // species
        if (SubmissionValidator.validateSpecies(getSpecies()).hasError()) {
            warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(speciesPanel, "Please choose at least one species");
            showWarnings();
            invalid = true;
        }

        // tissue
        if (!invalid && SubmissionValidator.validateTissues(getTissues()).hasError()) {
            warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(tissuePanel, "Please choose at least one tissue");
            showWarnings();
            invalid = true;
        }

        // cell type
        Set<CvParam> cellTypes = getCellTypes();
        if (!invalid && !cellTypes.isEmpty() && SubmissionValidator.validateCellTypes(cellTypes).hasError()) {
            warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(cellTypePanel, "Please choose cell type");
            showWarnings();
            invalid = true;
        }

        // disease
        Set<CvParam> diseases = getDiseases();
        if (!invalid && !diseases.isEmpty() && SubmissionValidator.validateDiseases(diseases).hasError()) {
            warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(diseasePanel, "Please choose disease");
            showWarnings();
            invalid = true;
        }

        // modification
        if (!invalid && SubmissionValidator.validateModifications(getModifications()).hasError()) {
            warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(modificationPanel, "Please choose modifications");
            showWarnings();
            invalid = true;
        }

        // instrument
        if (!invalid && SubmissionValidator.validateInstruments(getInstruments()).hasError()) {
            warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(instrumentPanel, "Please give at least one instrument");
            showWarnings();
            invalid = true;
        }

        // quantification
        Set<CvParam> quantifications = getQuantifications();
        if (!invalid && !quantifications.isEmpty() && SubmissionValidator.validateQuantifications(quantifications).hasError()) {
            warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(quantPanel, "Please choose quantifications");
            showWarnings();
            invalid = true;
        }

        // comment
//        boolean equals = appContext.getProperty("comment.default.text").equals(commentTextArea.getText());
//        ValidationReport validationReport = SubmissionValidator.validateReasonForPartialSubmission(commentTextArea.getText(), SubmissionTypeConstants.PARTIAL);
//        if (!invalid && (equals || validationReport.hasError())) {
//            warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(commentInputPanel, "Please leave a short comment on why submit unsupported result files");
//            showWarnings();
//            invalid = true;
//        }

        return invalid ? ValidationState.ERROR : ValidationState.SUCCESS;
    }

    /**
     * Close the warning balloon tip
     */
    public void hideWarnings() {
        if (warningBalloonTip != null && warningBalloonTip.isVisible()) {
            warningBalloonTip.closeBalloon();
        }
    }

    private void showWarnings() {
        warningBalloonTip.setVisible(true);
        this.revalidate();
        this.repaint();
    }

    public JPanel getModificationPanel() {
        return modificationPanel;
    }

    public void setModificationPanel(JPanel modificationPanel) {
        this.modificationPanel = modificationPanel;
    }

    public MetaDataTableModel getModificationTableModel() {
        return modificationTableModel;
    }

    public void setModificationTableModel(MetaDataTableModel modificationTableModel) {
        this.modificationTableModel = modificationTableModel;
    }
}
