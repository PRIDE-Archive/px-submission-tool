package uk.ac.ebi.pride.gui.form.dialog;

import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.SampleMetaData;
import uk.ac.ebi.pride.data.util.Constant;
import uk.ac.ebi.pride.data.util.ExperimentalFactorUtil;
import uk.ac.ebi.pride.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.form.combo.model.ResultFileSelectionModel;
import uk.ac.ebi.pride.gui.form.combo.renderer.ResultFileMetaDataComboRenderer;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareDialog;
import uk.ac.ebi.pride.gui.form.comp.HintedTextArea;
import uk.ac.ebi.pride.gui.form.comp.NonOpaquePanel;
import uk.ac.ebi.pride.gui.form.table.TableFactory;
import uk.ac.ebi.pride.gui.form.table.model.ResultFileMetaDataTableModel;
import uk.ac.ebi.pride.gui.navigation.NavigationControlPanel;
import uk.ac.ebi.pride.gui.util.BalloonTipUtil;
import uk.ac.ebi.pride.gui.util.BorderUtil;
import uk.ac.ebi.pride.prider.dataprovider.file.ProjectFileType;

import javax.help.HelpBroker;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class SampleMetaDataDialog extends ContextAwareDialog implements ActionListener {
    private static final String CANCEL_ACTION_COMMAND = "cancelAction";
    private static final String ADD_ACTION_COMMAND = "addAction";

    private static final float DEFAULT_TITLE_FONT_SIZE = 15f;

    private static final String APPLY_TO_ALL_LABEL = "Apply to all";

    /**
     * The data file which the mapping is going to be added
     */
    private DataFile dataFile;

    private JCheckBox speciesApplyToAll;
    private JCheckBox tissueApplyToAll;
    private JCheckBox cellTypeApplyToAll;
    private JCheckBox diseaseApplyToAll;
    private JCheckBox instrumentApplyToAll;
    private JCheckBox quantificationApplyToAll;

    private ResultFileMetaDataTableModel speciesTableModel;
    private ResultFileMetaDataTableModel tissueTableModel;
    private ResultFileMetaDataTableModel cellTypeTableModel;
    private ResultFileMetaDataTableModel diseaseTableModel;
    private ResultFileMetaDataTableModel instrumentTableModel;
    private ResultFileMetaDataTableModel quantTableModel;

    private JTextArea experimentalFactorTextArea;

    public SampleMetaDataDialog(Frame owner, DataFile dataFile) {
        super(owner);
        this.dataFile = dataFile;
        initComponents();
    }

    public SampleMetaDataDialog(Dialog owner, DataFile dataFile) {
        super(owner);
        this.dataFile = dataFile;
        initComponents();
    }

    /**
     * Create GUI components
     */
    private void initComponents() {
        this.setSize(new Dimension(850, 700));

        JPanel contentPanel = new JPanel(new BorderLayout());
        this.setContentPane(contentPanel);

        // create table panel
        initMetaDataPanel();

        // create button panel
        initControlPanel();

        this.setContentPane(contentPanel);
    }

    /**
     * Initialize table panel
     */
    private void initMetaDataPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // message panel
        JPanel messagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel messageLabel = new JLabel(appContext.getProperty("apply.all.message"));
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("apply.all.message.icon"));
        messageLabel.setIcon(icon);
        messagePanel.add(messageLabel);
        container.add(messagePanel, BorderLayout.NORTH);

        // cv param metadata selection panel
        JPanel metadataSelectPanel = new JPanel(new GridLayout(3, 2));

        // species
        speciesApplyToAll = new JCheckBox(APPLY_TO_ALL_LABEL);
        speciesTableModel = new ResultFileMetaDataTableModel(dataFile, SampleMetaData.Type.SPECIES);
        metadataSelectPanel.add(initMetadataPanel(appContext.getProperty("species.label.title"),
                true,
                appContext.getProperty("species.combobox.default.selection"),
                appContext.getProperty("species.combobox.other.species"),
                appContext.getProperty("species.combobox.default.species.file"),
                Constant.NEWT,
                speciesApplyToAll,
                speciesTableModel));

        // tissue
        tissueApplyToAll = new JCheckBox(APPLY_TO_ALL_LABEL);
        tissueTableModel = new ResultFileMetaDataTableModel(dataFile, SampleMetaData.Type.TISSUE);
        metadataSelectPanel.add(initMetadataPanel(appContext.getProperty("tissue.label.title"),
                true,
                appContext.getProperty("tissue.combobox.default.selection"),
                appContext.getProperty("tissue.combobox.other.tissue"),
                appContext.getProperty("tissue.combobox.default.tissue.file"),
                Constant.BTO,
                tissueApplyToAll,
                tissueTableModel));

        // instrument
        instrumentApplyToAll = new JCheckBox(APPLY_TO_ALL_LABEL);
        instrumentTableModel = new ResultFileMetaDataTableModel(dataFile, SampleMetaData.Type.INSTRUMENT);
        metadataSelectPanel.add(initMetadataPanel(appContext.getProperty("instrument.label.title"),
                true,
                appContext.getProperty("instrument.combobox.default.selection"),
                appContext.getProperty("instrument.combobox.other.instrument"),
                appContext.getProperty("instrument.combobox.default.instrument.file"),
                Constant.MS,
                instrumentApplyToAll,
                instrumentTableModel));

        // cell type
        cellTypeApplyToAll = new JCheckBox(APPLY_TO_ALL_LABEL);
        cellTypeTableModel = new ResultFileMetaDataTableModel(dataFile, SampleMetaData.Type.CELL_TYPE);
        metadataSelectPanel.add(initMetadataPanel(appContext.getProperty("celltype.label.title"),
                false,
                appContext.getProperty("celltype.combobox.default.selection"),
                appContext.getProperty("celltype.combobox.other.celltype"),
                appContext.getProperty("celltype.combobox.default.celltype.file"),
                Constant.CL,
                cellTypeApplyToAll,
                cellTypeTableModel));

        // disease
        diseaseApplyToAll = new JCheckBox(APPLY_TO_ALL_LABEL);
        diseaseTableModel = new ResultFileMetaDataTableModel(dataFile, SampleMetaData.Type.DISEASE);
        metadataSelectPanel.add(initMetadataPanel(appContext.getProperty("disease.label.title"),
                false,
                appContext.getProperty("disease.combobox.default.selection"),
                appContext.getProperty("disease.combobox.other.disease"),
                appContext.getProperty("disease.combobox.default.disease.file"),
                Constant.DOID,
                diseaseApplyToAll,
                diseaseTableModel));

        // quantification
        quantificationApplyToAll = new JCheckBox(APPLY_TO_ALL_LABEL);
        quantTableModel = new ResultFileMetaDataTableModel(dataFile, SampleMetaData.Type.QUANTIFICATION_METHOD);
        metadataSelectPanel.add(initMetadataPanel(appContext.getProperty("quantification.label.title"),
                false,
                appContext.getProperty("quantification.combobox.default.selection"),
                appContext.getProperty("quantification.combobox.other.quantification"),
                appContext.getProperty("quantification.combobox.default.quantification.file"),
                Constant.PRIDE,
                quantificationApplyToAll,
                quantTableModel));

        container.add(metadataSelectPanel, BorderLayout.CENTER);

        // experimental factor panel
        JPanel experimentalFactorPanel = initExperimentalFactorPanel();
        container.add(experimentalFactorPanel, BorderLayout.SOUTH);

        this.getContentPane().add(container, BorderLayout.CENTER);
    }

    /**
     * Initialize metadata details panel
     */
    private JPanel initMetadataPanel(String panelTitle,
                                     boolean required,
                                     String defaultSelectionLabel,
                                     String otherSelectionLabel,
                                     String defaultValueFile,
                                     String ontology,
                                     JCheckBox applyToAll,
                                     ResultFileMetaDataTableModel tableModel) {
        JPanel metaDataPanel = new JPanel(new BorderLayout());

        // metadata label
        JPanel titlePanel = new JPanel(new BorderLayout());
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        labelPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 2, 0));

        JLabel metaDataPanelLabel = new JLabel(panelTitle + (required ? "*" : ""));
        metaDataPanelLabel.setFont(metaDataPanelLabel.getFont().deriveFont(DEFAULT_TITLE_FONT_SIZE).deriveFont(required ? Font.BOLD : Font.PLAIN));
        labelPanel.add(metaDataPanelLabel);
        titlePanel.add(labelPanel, BorderLayout.WEST);

        JPanel applyToAllPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        applyToAllPanel.add(applyToAll);
        applyToAllPanel.add(Box.createRigidArea(new Dimension(20, 5)));
        titlePanel.add(applyToAllPanel, BorderLayout.EAST);
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
    private JPanel initExperimentalFactorPanel() {
        JPanel experimentalFactorPanel = new JPanel(new BorderLayout());

        // modification label
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        labelPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 2, 0));

        // experimental factor label
        JLabel modLabel = new JLabel(appContext.getProperty("experimental.factor.label.title"));
        modLabel.setFont(modLabel.getFont().deriveFont(DEFAULT_TITLE_FONT_SIZE));
        labelPanel.add(modLabel);

        // experimental factor help
        JLabel helpLabel = new JLabel();
        helpLabel.setIcon(GUIUtilities.loadIcon(appContext.getProperty("experimental.factor.label.help.icon")));
        helpLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                HelpBroker hb = appContext.getMainHelpBroker();
                hb.showID("help.experimental.factor", "javax.help.SecondaryWindow", "main");
            }

        });
        labelPanel.add(helpLabel);

        experimentalFactorPanel.add(labelPanel, BorderLayout.NORTH);

        JPanel experimentalFactorInputPanel = new JPanel(new BorderLayout());
        experimentalFactorInputPanel.setBorder(BorderUtil.createLoweredBorder());

        experimentalFactorTextArea = new HintedTextArea(appContext.getProperty("experimental.factor.default.text"));
        SampleMetaData sampleMetaData = dataFile.getSampleMetaData();
        if (sampleMetaData != null) {
            Set<CvParam> params = sampleMetaData.getMetaData(SampleMetaData.Type.EXPERIMENTAL_FACTOR);
            if (params != null && !params.isEmpty()) {
                String value = params.iterator().next().getValue();
                // This is a special case, we are adding default value when there is no experimental factor specified
                // In the submission tool, the experimental factor is not mandatory
                if (!value.equalsIgnoreCase(appContext.getProperty("experimental.factor.default.value"))) {
                    experimentalFactorTextArea.setText(value);
                }
            }
        }
        BalloonTipUtil.createBalloonTooltip(experimentalFactorTextArea, appContext.getProperty("experimental.factor.tooltip"));

        JScrollPane scrollPane = new JScrollPane(experimentalFactorTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        experimentalFactorInputPanel.add(scrollPane, BorderLayout.CENTER);
        experimentalFactorPanel.add(experimentalFactorInputPanel, BorderLayout.CENTER);

        return experimentalFactorPanel;
    }

    /**
     * Initialize control panel
     */
    private void initControlPanel() {
        // setup main pane
        JPanel controlPanel = new NavigationControlPanel();
        controlPanel.setLayout(new BorderLayout());

//        // help button
//        JPanel helpButtonPanel = new NonOpaquePanel(new FlowLayout(FlowLayout.LEFT));
//        JButton helpButton = GUIUtilities.createLabelLikeButton(GUIUtilities.loadIcon(appContext.getProperty("help.button.small.icon")), null);
//        helpButton.setPreferredSize(new Dimension(90, 30));
//        helpButton.setFocusable(false);
//        helpButton.setActionCommand(HELP_ACTION_COMMAND);
//        helpButton.addActionListener(this);
//        helpButtonPanel.add(helpButton);
//        controlPanel.add(helpButtonPanel, BorderLayout.WEST);

        // control pane
        JPanel ctrlPane = new NonOpaquePanel(new FlowLayout(FlowLayout.RIGHT));

        // cancel button
        JButton cancelButton = new JButton(appContext.getProperty("cancel.button.label"),
                GUIUtilities.loadIcon(appContext.getProperty("cancel.button.small.icon")));
        cancelButton.setPreferredSize(new Dimension(90, 30));
        cancelButton.setActionCommand(CANCEL_ACTION_COMMAND);
        cancelButton.addActionListener(this);
        ctrlPane.add(cancelButton);

        // next button
        JButton addButton = new JButton(appContext.getProperty("add.file.mapping.button.label"),
                GUIUtilities.loadIcon(appContext.getProperty("add.file.mapping.dialog.button.small.icon")));
        addButton.setPreferredSize(new Dimension(90, 30));
        addButton.setActionCommand(ADD_ACTION_COMMAND);
        addButton.addActionListener(this);
        ctrlPane.add(addButton);

        controlPanel.add(ctrlPane, BorderLayout.EAST);

        this.getContentPane().add(controlPanel, BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String evtName = e.getActionCommand();

        if (CANCEL_ACTION_COMMAND.equals(evtName)) {
            this.dispose();
        } else if (ADD_ACTION_COMMAND.equals(evtName)) {
            updateDataFile();
            this.dispose();
        }
    }

    private void updateDataFile() {
        // check species apply to all
        if (speciesApplyToAll.isSelected()) {
            java.util.List<DataFile> resultFiles = appContext.getSubmissionFilesByType(ProjectFileType.RESULT);
            for (DataFile resultFile : resultFiles) {
                appContext.setSampleMetaDataEntries(resultFile, SampleMetaData.Type.SPECIES, speciesTableModel.getValues());
            }
        } else {
            appContext.setSampleMetaDataEntries(dataFile, SampleMetaData.Type.SPECIES, speciesTableModel.getValues());
        }

        // check tissue apply to all
        if (tissueApplyToAll.isSelected()) {
            java.util.List<DataFile> resultFiles = appContext.getSubmissionFilesByType(ProjectFileType.RESULT);
            for (DataFile resultFile : resultFiles) {
                appContext.setSampleMetaDataEntries(resultFile, SampleMetaData.Type.TISSUE, tissueTableModel.getValues());
            }
        } else {
            appContext.setSampleMetaDataEntries(dataFile, SampleMetaData.Type.TISSUE, tissueTableModel.getValues());
        }

        // check cell type apply to all
        if (cellTypeApplyToAll.isSelected()) {
            java.util.List<DataFile> resultFiles = appContext.getSubmissionFilesByType(ProjectFileType.RESULT);
            for (DataFile resultFile : resultFiles) {
                appContext.setSampleMetaDataEntries(resultFile, SampleMetaData.Type.CELL_TYPE, cellTypeTableModel.getValues());
            }
        } else {
            appContext.setSampleMetaDataEntries(dataFile, SampleMetaData.Type.CELL_TYPE, cellTypeTableModel.getValues());
        }

        // check disease apply to all
        if (diseaseApplyToAll.isSelected()) {
            java.util.List<DataFile> resultFiles = appContext.getSubmissionFilesByType(ProjectFileType.RESULT);
            for (DataFile resultFile : resultFiles) {
                appContext.setSampleMetaDataEntries(resultFile, SampleMetaData.Type.DISEASE, diseaseTableModel.getValues());
            }
        } else {
            appContext.setSampleMetaDataEntries(dataFile, SampleMetaData.Type.DISEASE, diseaseTableModel.getValues());
        }

        // check instrument apply to all
        if (instrumentApplyToAll.isSelected()) {
            java.util.List<DataFile> resultFiles = appContext.getSubmissionFilesByType(ProjectFileType.RESULT);
            for (DataFile resultFile : resultFiles) {
                appContext.setSampleMetaDataEntries(resultFile, SampleMetaData.Type.INSTRUMENT, instrumentTableModel.getValues());
            }
        } else {
            appContext.setSampleMetaDataEntries(dataFile, SampleMetaData.Type.INSTRUMENT, instrumentTableModel.getValues());
        }

        // check quantification apply to all
        if (quantificationApplyToAll.isSelected()) {
            java.util.List<DataFile> resultFiles = appContext.getSubmissionFilesByType(ProjectFileType.RESULT);
            for (DataFile resultFile : resultFiles) {
                appContext.setSampleMetaDataEntries(resultFile, SampleMetaData.Type.QUANTIFICATION_METHOD, quantTableModel.getValues());
            }
        } else {
            appContext.setSampleMetaDataEntries(dataFile, SampleMetaData.Type.QUANTIFICATION_METHOD, quantTableModel.getValues());
        }

        // add experimental factor
        String text = experimentalFactorTextArea.getText();
        String experimentalFactor = text == null ? "" : text.trim();
        if (!appContext.getProperty("experimental.factor.default.text").equals(experimentalFactor)) {
            Set<CvParam> experimentalFactors = new HashSet<CvParam>();
            if (!"".equals(experimentalFactor)) {
                experimentalFactors.add(ExperimentalFactorUtil.getExperimentalFactorCvParam(experimentalFactor));
            }
            appContext.setSampleMetaDataEntries(dataFile, SampleMetaData.Type.EXPERIMENTAL_FACTOR, experimentalFactors);
        }
    }
}
