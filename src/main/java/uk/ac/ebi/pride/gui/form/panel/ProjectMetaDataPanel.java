/*
 * Created by JFormDesigner on Fri Oct 28 09:20:24 BST 2011
 */

package uk.ac.ebi.pride.gui.form.panel;

import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.ProjectMetaData;
import uk.ac.ebi.pride.data.util.MassSpecExperimentMethod;
import uk.ac.ebi.pride.data.validation.SubmissionValidator;
import uk.ac.ebi.pride.gui.util.WarningMessageGenerator;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.form.combo.model.ExperimentMethodSelectionModel;
import uk.ac.ebi.pride.gui.form.combo.renderer.ExperimentMethodComboRenderer;
import uk.ac.ebi.pride.gui.form.comp.ContextAwarePanel;
import uk.ac.ebi.pride.gui.form.comp.HintedTextArea;
import uk.ac.ebi.pride.gui.form.comp.HintedTextField;
import uk.ac.ebi.pride.gui.form.table.TableFactory;
import uk.ac.ebi.pride.gui.form.table.model.ExperimentMethodTableModel;
import uk.ac.ebi.pride.gui.util.BalloonTipUtil;
import uk.ac.ebi.pride.gui.util.ColourUtil;
import uk.ac.ebi.pride.gui.util.ValidationState;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;

/**
 * Panel for capturing experiment details
 *
 * @author Rui Wang
 * @version $Id$
 */
public class ProjectMetaDataPanel extends ContextAwarePanel {

    private String projectTitleHint;
    private String projectKeywordsHint;
    private String projectDescHint;
    private String sampleProcessingHint;
    private String dataProcessingHint;

    public ProjectMetaDataPanel() {

        initComponents();
        initTooltips();
    }

    public String getProjectTitle() {
        return projectTitleField.getText();
    }

    public void setProjectTitle(String projectTitle) {
        projectTitleField.setText(projectTitle);
    }

    public String getKeywords() {
        return keywordField.getText();
    }

    public void setKeywords(String keywords) {
        keywordField.setText(keywords);
    }

    public String getProjectDesc() {
        return projectDescEditorPane.getText();
    }

    public void setProjectDesc(String projectDesc) {
        projectDescEditorPane.setText(projectDesc);
    }

    public String getSampleProcessingProtocol() {
        return sampleProcessingProtocolEditorPane.getText();
    }

    public void setSampleProcessingProtocol(String sampleProcessing) {
        sampleProcessingProtocolEditorPane.setText(sampleProcessing);
    }

    public String getDataProcessingProtocol() {
        return dataProcessingProtocolEditorPane.getText();
    }

    public void setDataProcessingProtocol(String dataProcessing) {
        dataProcessingProtocolEditorPane.setText(dataProcessing);
    }

    /**
     * Close the warning balloon tip
     */
    public void hideWarnings() {
        if (warningBalloonTip != null && warningBalloonTip.isVisible()) {
            warningBalloonTip.closeBalloon();
        }
        projectTitleField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        keywordField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        projectDescEditorPane.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        sampleProcessingProtocolEditorPane.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        dataProcessingProtocolEditorPane.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        experimentMethodTable.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
    }

    public ValidationState doValidation(boolean showWarning) {
        boolean inValid = false;

        if (warningBalloonTip != null) {
            warningBalloonTip.closeBalloon();
        }

        // experiment field
        String title = projectTitleField.getText();
        if (projectTitleHint.equals(title) || SubmissionValidator.validateProjectTile(title).hasError()) {
            if (showWarning) {
                warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(projectTitleField, appContext.getProperty("project.title.error.message"));
                warningBalloonTip.setVisible(true);
            }
            projectTitleField.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
            inValid = true;
        } else {
            projectTitleField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        }

        // keyword field
        String keywords = keywordField.getText();
        if (projectKeywordsHint.equals(keywords) || SubmissionValidator.validateKeywords(keywords).hasError()) {
            if (!inValid && showWarning) {
                warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(keywordField, appContext.getProperty("keyword.error.message"));
                warningBalloonTip.setVisible(true);
            }
            keywordField.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
            inValid = true;
        } else {
            keywordField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        }

        // project description
        String projectDesc = projectDescEditorPane.getText();
        if (projectDescHint.equals(projectDesc) || SubmissionValidator.validateProjectDescription(projectDesc).hasError()) {
            if (!inValid && showWarning) {
                warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(projectDescEditorPane, appContext.getProperty("project.desc.error.message"));
                warningBalloonTip.setVisible(true);
            }
            projectDescEditorPane.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
            inValid = true;
        } else {
            projectDescEditorPane.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        }

        // sample processing protocol
        String sampleProcessing = sampleProcessingProtocolEditorPane.getText();
        if (this.sampleProcessingHint.equals(sampleProcessing) || SubmissionValidator.validateSampleProcessingProtocol(sampleProcessing).hasError()) {
            if (!inValid && showWarning) {
                warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(sampleProcessingProtocolEditorPane, appContext.getProperty("sample.processing.error.message"));
                warningBalloonTip.setVisible(true);
            }
            sampleProcessingProtocolEditorPane.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
            inValid = true;
        } else {
            sampleProcessingProtocolEditorPane.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        }

        // data processing protocol
        String dataProcessing = dataProcessingProtocolEditorPane.getText();
        if (this.dataProcessingHint.equals(dataProcessing) || SubmissionValidator.validateSampleProcessingProtocol(dataProcessing).hasError()) {
            if (!inValid && showWarning) {
                warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(dataProcessingProtocolEditorPane, appContext.getProperty("data.processing.error.message"));
                warningBalloonTip.setVisible(true);
            }
            dataProcessingProtocolEditorPane.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
            inValid = true;
        } else {
            dataProcessingProtocolEditorPane.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        }

        // mass spec experiment method
        if (SubmissionValidator.validateExperimentMethods(appContext.getSubmissionRecord().getSubmission().getProjectMetaData().getMassSpecExperimentMethods()).hasError()) {
            if (!inValid && showWarning) {
                warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(experimentMethodComboBox, appContext.getProperty("mass.spec.experiment.method.error.message"));
                warningBalloonTip.setVisible(true);
            }
            experimentMethodTable.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
            inValid = true;
        } else {
            experimentMethodTable.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        }

        if (isCrosslinkDataset(appContext.getSubmissionRecord().getSubmission().getProjectMetaData(),
        title, keywords, projectDesc)){
            JLabel label = new JLabel();
            Font font = label.getFont();
            StringBuilder style = new StringBuilder("font-family:" + font.getFamily() + ";");
            style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
            style.append("font-size:" + font.getSize() + "pt;");
            JEditorPane jEditorPane = new JEditorPane("text/html", "<html><body style=\"" + style + "\">" //
                    + WarningMessageGenerator.getCrosslinkingWarning(appContext.getSubmissionRecord().getSubmission().getProjectMetaData().getSubmissionType()) + "</body></html>");
            jEditorPane.addHyperlinkListener(e -> {
                try {
                    if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
                        Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
//                    logger.error(ex.getMessage());
                }
            });
            jEditorPane.setEditable(false);
            jEditorPane.setBackground(label.getBackground());
            JOptionPane.showConfirmDialog(app.getMainFrame(),
                    jEditorPane,
                    appContext.getProperty("crosslinking.detected.dialog.title"),
                    JOptionPane.CLOSED_OPTION, JOptionPane.WARNING_MESSAGE);
        }

        return inValid ? ValidationState.ERROR : ValidationState.SUCCESS;
    }

    private boolean isCrosslinkDataset(ProjectMetaData projectMetaData, String title, String keywords, String projectDesc){
        boolean isCrosslinkDataset = false;
        if(!projectMetaData.getMassSpecExperimentMethods().isEmpty()){
            for(CvParam cvParam:projectMetaData.getMassSpecExperimentMethods()){
                if(cvParam.getAccession().equals("PRIDE:0000430")){
                    isCrosslinkDataset = true;
                }
            }
        }
        if(title.toLowerCase().contains("crosslink") || title.toLowerCase().contains("cross-link")){
            isCrosslinkDataset = true;
        }
        if(keywords.toLowerCase().contains("crosslink") || keywords.toLowerCase().contains("cross-link")){
            isCrosslinkDataset = true;
        }
        if(projectDesc.toLowerCase().contains("crosslink") || projectDesc.toLowerCase().contains("cross-link")){
            isCrosslinkDataset = true;
        }
        return isCrosslinkDataset;
    }

    private void initTooltips() {
        BalloonTipUtil.createBalloonTooltip(projectTitleField, appContext.getProperty("project.title.tooltip"));
        BalloonTipUtil.createBalloonTooltip(keywordField, appContext.getProperty("keyword.tooltip"));
        BalloonTipUtil.createBalloonTooltip(projectDescEditorPane, appContext.getProperty("project.desc.tooltip"));
        BalloonTipUtil.createBalloonTooltip(sampleProcessingProtocolEditorPane, appContext.getProperty("sample.processing.tooltip"));
        BalloonTipUtil.createBalloonTooltip(dataProcessingProtocolEditorPane, appContext.getProperty("data.processing.tooltip"));
    }

    private void createUIComponents() {

        String keyboardTip = isMac() ? appContext.getProperty("copy.paste.tip.for.mac") : "";
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("copy.paste.tip.for.mac.icon"));
        tipLabel = new JLabel(keyboardTip);
        tipLabel.setIcon(icon);

        this.projectTitleHint = appContext.getProperty("project.title.hint");
        projectTitleField = new HintedTextField(projectTitleHint);

        this.projectKeywordsHint = appContext.getProperty("project.keywords.hint");
        keywordField = new HintedTextField(projectKeywordsHint);

        this.projectDescHint = appContext.getProperty("project.description.hint");
        projectDescEditorPane = new HintedTextArea(projectDescHint);

        this.sampleProcessingHint = appContext.getProperty("sample.processing.hint");
        sampleProcessingProtocolEditorPane = new HintedTextArea(sampleProcessingHint);

        this.dataProcessingHint = appContext.getProperty("data.processing.hint");
        dataProcessingProtocolEditorPane = new HintedTextArea(dataProcessingHint);

        experimentMethodComboBox = new JComboBox();
        experimentMethodComboBox.setFont(experimentMethodComboBox.getFont().deriveFont(14f));
        experimentMethodComboBox.setModel(new ExperimentMethodSelectionModel());
        experimentMethodComboBox.setRenderer(new ExperimentMethodComboRenderer());
        ExperimentMethodTableModel experimentMethodTableModel = new ExperimentMethodTableModel();
        experimentMethodTable = TableFactory.createMetadataTable(experimentMethodTableModel);
    }

    private boolean isMac() {
        String OS = System.getProperty("os.name").toLowerCase();
        return OS.contains("mac");
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        createUIComponents();

        projectTitleLabel = new JLabel();
        keywordLabel = new JLabel();
        projectDescLabel = new JLabel();
        scrollPane1 = new JScrollPane();
        sampleProcessingProtocolLabel = new JLabel();
        scrollPane2 = new JScrollPane();
        dataProcessingProtocolLabel = new JLabel();
        scrollPane3 = new JScrollPane();
        expermentMethodLabel = new JLabel();
        scrollPane4 = new JScrollPane();
        label1 = new JLabel();
        label2 = new JLabel();
        label3 = new JLabel();
        label4 = new JLabel();

        //======== this ========

        //---- projectTitleLabel ----
        projectTitleLabel.setText("Project title*");
        projectTitleLabel.setFont(new Font("sansserif", Font.PLAIN, 14));

        //---- keywordLabel ----
        keywordLabel.setText("Keywords*");
        keywordLabel.setFont(new Font("sansserif", Font.PLAIN, 14));

        //---- projectDescLabel ----
        projectDescLabel.setText("Project description* ");
        projectDescLabel.setFont(new Font("sansserif", Font.PLAIN, 14));

        //======== scrollPane1 ========
        {
            scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- projectDescEditorPane ----
            projectDescEditorPane.setLineWrap(true);
            scrollPane1.setViewportView(projectDescEditorPane);
        }

        //---- sampleProcessingProtocolLabel ----
        sampleProcessingProtocolLabel.setText("Sample processing protocol*");
        sampleProcessingProtocolLabel.setFont(new Font("sansserif", Font.PLAIN, 14));

        //======== scrollPane2 ========
        {
            scrollPane2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- sampleProcessingProtocolEditorPane ----
            sampleProcessingProtocolEditorPane.setLineWrap(true);
            scrollPane2.setViewportView(sampleProcessingProtocolEditorPane);
        }

        //---- dataProcessingProtocolLabel ----
        dataProcessingProtocolLabel.setText("Data processing protocol*");
        dataProcessingProtocolLabel.setFont(new Font("sansserif", Font.PLAIN, 14));

        //======== scrollPane3 ========
        {
            scrollPane3.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- dataProcessingProtocolEditorPane ----
            dataProcessingProtocolEditorPane.setLineWrap(true);
            scrollPane3.setViewportView(dataProcessingProtocolEditorPane);
        }

        //---- expermentMethodLabel ----
        expermentMethodLabel.setText("Experiment type*");
        expermentMethodLabel.setFont(new Font("sansserif", Font.PLAIN, 14));

        //======== scrollPane4 ========
        {
            scrollPane4.setViewportView(experimentMethodTable);
        }

        //---- label1 ----
        label1.setText("(50 to 5000 characters)");
        label1.setForeground(new Color(102, 102, 102));

        //---- label2 ----
        label2.setText("(50 to 5000 characters)");
        label2.setForeground(new Color(102, 102, 102));

        //---- label3 ----
        label3.setText("(50 to 5000 characters)");
        label3.setForeground(new Color(102, 102, 102));

        //---- label4 ----
        label4.setText("(30 to 5000 characters)");
        label4.setForeground(new Color(102, 102, 102));

        //---- tipLabel ----
        tipLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup()
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup()
                                        .addComponent(experimentMethodComboBox)
                                        .addComponent(scrollPane4, GroupLayout.Alignment.TRAILING)
                                        .addComponent(scrollPane1, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
                                        .addComponent(scrollPane2, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
                                        .addComponent(scrollPane3, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup()
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(projectTitleLabel)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(label4, GroupLayout.PREFERRED_SIZE, 184, GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(keywordLabel)
                                                        .addComponent(keywordField, GroupLayout.PREFERRED_SIZE, 352, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(projectTitleField, GroupLayout.PREFERRED_SIZE, 483, GroupLayout.PREFERRED_SIZE)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(sampleProcessingProtocolLabel)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(label2, GroupLayout.PREFERRED_SIZE, 184, GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(dataProcessingProtocolLabel)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(label3, GroupLayout.PREFERRED_SIZE, 169, GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(expermentMethodLabel)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(projectDescLabel)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(label1, GroupLayout.PREFERRED_SIZE, 336, GroupLayout.PREFERRED_SIZE)))
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(tipLabel, GroupLayout.PREFERRED_SIZE, 500, GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup()
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(projectTitleLabel)
                                        .addComponent(label4)
                                        .addComponent(tipLabel))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(projectTitleField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(keywordLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(keywordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(projectDescLabel)
                                        .addComponent(label1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrollPane1, GroupLayout.PREFERRED_SIZE, 64, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(sampleProcessingProtocolLabel)
                                        .addComponent(label2))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrollPane2, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(dataProcessingProtocolLabel)
                                        .addComponent(label3))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrollPane3, GroupLayout.PREFERRED_SIZE, 62, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(expermentMethodLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(experimentMethodComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrollPane4, GroupLayout.DEFAULT_SIZE, 35, Short.MAX_VALUE)
                                .addContainerGap())
        );
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JLabel projectTitleLabel;
    private JTextField projectTitleField;
    private JLabel keywordLabel;
    private JTextField keywordField;
    private JLabel projectDescLabel;
    private JScrollPane scrollPane1;
    private JTextArea projectDescEditorPane;
    private JLabel sampleProcessingProtocolLabel;
    private JScrollPane scrollPane2;
    private JTextArea sampleProcessingProtocolEditorPane;
    private JLabel dataProcessingProtocolLabel;
    private JScrollPane scrollPane3;
    private JTextArea dataProcessingProtocolEditorPane;
    private JLabel expermentMethodLabel;
    private JComboBox experimentMethodComboBox;
    private JScrollPane scrollPane4;
    private JTable experimentMethodTable;
    private JLabel label1;
    private JLabel label2;
    private JLabel label3;
    private JLabel label4;
    private JLabel tipLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
