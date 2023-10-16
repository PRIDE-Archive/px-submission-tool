/*
 * Created by JFormDesigner on Tue Aug 13 15:33:05 BST 2013
 */

package uk.ac.ebi.pride.gui.form.panel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.validation.SubmissionValidator;
import uk.ac.ebi.pride.gui.form.comp.ContextAwarePanel;
import uk.ac.ebi.pride.gui.form.comp.HintedTextField;
import uk.ac.ebi.pride.gui.form.table.TableFactory;
import uk.ac.ebi.pride.gui.form.table.model.ProjectTagTableModel;
import uk.ac.ebi.pride.gui.util.*;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @author User #2
 */
public class AdditionalProjectMetadataPanel extends ContextAwarePanel {
    private static final Logger logger = LoggerFactory.getLogger(AdditionalProjectMetadataPanel.class);


    private ProjectTagTableModel parentProjectTableModel;
    private String pubmedIdHint;
    private String reanalysisPXAccessionHint;
    private String otherOmicsDatasetLinkHint;

    public AdditionalProjectMetadataPanel() {

        initComponents();

        initTooltips();
    }

    private void createUIComponents() {
        String defaultValueFile = appContext.getProperty("project.tag.default.tag.file");

        Collection<String> defaultParentProjects = null;
        try {
            defaultParentProjects = CvFileUtil.parseByLine(defaultValueFile);
        } catch (IOException e) {
            logger.error("Failed to parse default project tags from file", e);
        }

        // I do it this way because, apparently, the original developer wanted to preserve insertion order, for whatever
        // reasong, when reading the tags, so I won't change that, but I'll change this because we want those tags to
        // show up in alphabetical order. I don't like it, but it'll do the job
        List<String> cvs = new ArrayList<>(defaultParentProjects);
        Collections.sort(cvs);
        defaultParentProjects = cvs;

        String parentProjectDescription = appContext.getProperty("project.tag.desc.message");
        parentProjectDescLabel = new JLabel(parentProjectDescription);

        parentProjectTableModel = new ProjectTagTableModel(defaultParentProjects);
        parentProjectTable = TableFactory.createParentProjectTable(parentProjectTableModel);

        pubmedIdHint = appContext.getProperty("pubmed.id.hint");
        pubmedIdTextField = new HintedTextField(pubmedIdHint);

        reanalysisPXAccessionHint = appContext.getProperty("px.acc.reanalysis.hint");
        reanalysisPXAccessionTextField = new HintedTextField(reanalysisPXAccessionHint);

        otherOmicsDatasetLinkHint = appContext.getProperty("other.omics.link.hint");
        otherOmicsDatasetLinkTextField = new HintedTextField(otherOmicsDatasetLinkHint);
    }


    public ValidationState doValidation(boolean showWarning) {
        boolean invalid = false;

        if (warningBalloonTip != null) {
            warningBalloonTip.closeBalloon();
        }

        // pubmed
        String pubMedIds = pubmedIdTextField.getText();
        if (pubMedIds != null && !pubmedIdHint.equals(pubMedIds)) {
            Matcher m = Constant.PUBMED_PATTERN.matcher(pubMedIds);
            if (m.find()) {
                pubmedIdTextField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
            } else {
                if (showWarning) {
                    warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(pubmedIdTextField, appContext.getProperty("pubmed.id.error.message"));
                    warningBalloonTip.setVisible(true);
                }
                pubmedIdTextField.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
                invalid = true;
            }
        }

        // reanalysis px accessions
        String pxAccs = reanalysisPXAccessionTextField.getText();
        if (pxAccs != null && !reanalysisPXAccessionHint.equals(pxAccs)) {
            Matcher m = Constant.REANALYSIS_PX_ACC_PATTERN.matcher(pxAccs);
            if (m.find()) {
                reanalysisPXAccessionTextField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
            } else {
                if (!invalid && showWarning) {
                    warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(reanalysisPXAccessionTextField, appContext.getProperty("px.acc.reanalysis.error.message"));
                    warningBalloonTip.setVisible(true);
                }
                reanalysisPXAccessionTextField.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
                invalid = true;
            }
        }

        // other omics dataset links
        String otherOmicsLink = otherOmicsDatasetLinkTextField.getText();
        if (otherOmicsLink != null && !otherOmicsDatasetLinkHint.equals(otherOmicsLink)) {
            if (SubmissionValidator.validateOtherOmicsLink(otherOmicsLink).hasError()) {
                if (!invalid && showWarning) {
                    warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(otherOmicsDatasetLinkTextField, appContext.getProperty("other.omics.link.error.message"));
                    warningBalloonTip.setVisible(true);
                }
                otherOmicsDatasetLinkTextField.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
                invalid = true;
            } else {
                otherOmicsDatasetLinkTextField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
            }
        }


        return invalid ? ValidationState.ERROR : ValidationState.SUCCESS;
    }

    public void hideWarnings() {
        if (warningBalloonTip != null && warningBalloonTip.isVisible()) {
            warningBalloonTip.closeBalloon();
        }
        parentProjectTable.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        pubmedIdTextField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        reanalysisPXAccessionTextField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
        otherOmicsDatasetLinkTextField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
    }

    private void initTooltips() {
        BalloonTipUtil.createBalloonTooltip(parentProjectTable, appContext.getProperty("project.tag.tooltip"));
        BalloonTipUtil.createBalloonTooltip(pubmedIdTextField, appContext.getProperty("pubmed.id.tooltip"));
        BalloonTipUtil.createBalloonTooltip(reanalysisPXAccessionTextField, appContext.getProperty("px.acc.reanalysis.tooltip"));
        BalloonTipUtil.createBalloonTooltip(otherOmicsDatasetLinkTextField, appContext.getProperty("other.omics.link.tooltip"));
    }

    public Set<String> getSelectedProjectTags() {
        return parentProjectTableModel.getSelectedProjectTags();
    }

    public void setSelectedProjectTags(Collection<String> projectTags) {
        parentProjectTableModel.setSelectedProjectTags(projectTags);
    }

    public Set<String> getPubMedIds() {
        Set<String> parsedPubMedIds = new HashSet<>();

        String pubMedIds = pubmedIdTextField.getText();
        if (pubMedIds != null) {
            parsedPubMedIds.addAll(StringUtil.splitString(pubMedIds, StringUtil.COMMA_WHITESPACE_SPLIT_PATTERN));
        }

        return parsedPubMedIds;
    }

    public void setPubMedIds(Collection<String> pubMedIds) {
        String pubmedText = StringUtil.concatenateStrings(pubMedIds);
        pubmedIdTextField.setText(pubmedText);
    }

    public Set<String> getReanalysisPXAccessions() {
        Set<String> parsedReanalysisPXAccessions = new HashSet<>();

        String reanalysisPXAccessions = reanalysisPXAccessionTextField.getText();
        if (reanalysisPXAccessions != null) {
            parsedReanalysisPXAccessions.addAll(StringUtil.splitString(reanalysisPXAccessions, StringUtil.COMMA_WHITESPACE_SPLIT_PATTERN));
        }

        return parsedReanalysisPXAccessions;
    }

    public void setReanalysisPXAccessions(Collection<String> pxAccessions) {
        String pxAccessionText = StringUtil.concatenateStrings(pxAccessions);
        reanalysisPXAccessionTextField.setText(pxAccessionText);
    }

    public String getOtherOmicsDatasetLink() {
        return otherOmicsDatasetLinkTextField.getText();
    }

    public void setOtherOmicsDatasetLink(String otherOmicsDatasetLink) {
        otherOmicsDatasetLinkTextField.setText(otherOmicsDatasetLink);
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        createUIComponents();

        parentProjectLabel = new JLabel();
        scrollPane1 = new JScrollPane();
        pubmedIdLabel = new JLabel();
        reanalysisPXAccessionLabel = new JLabel();
        otherOmicsDatasetLinkLabel = new JLabel();
        label1 = new JLabel();
        label2 = new JLabel();
        label3 = new JLabel();
        label4 = new JLabel();

        //======== this ========

        //---- parentProjectLabel ----
        parentProjectLabel.setText("Parent project");
        parentProjectLabel.setFont(new Font("sansserif", Font.PLAIN, 14));

        //======== scrollPane1 ========
        {
            scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //---- parentProjectTable ----
            parentProjectTable.setFillsViewportHeight(true);
            scrollPane1.setViewportView(parentProjectTable);
        }

        //---- pubmedIdLabel ----
        pubmedIdLabel.setText("PubMed ID(s)");
        pubmedIdLabel.setFont(new Font("sansserif", Font.PLAIN, 14));

        //---- reanalysisPXAccessionLabel ----
        reanalysisPXAccessionLabel.setText("Reanalysis ProtemeXchange accession(s)");
        reanalysisPXAccessionLabel.setFont(new Font("sansserif", Font.PLAIN, 14));

        //---- otherOmicsDatasetLinkLabel ----
        otherOmicsDatasetLinkLabel.setText("Links to other 'Omics' datasets");
        otherOmicsDatasetLinkLabel.setFont(new Font("sansserif", Font.PLAIN, 14));

        //---- label1 ----
        label1.setText("(optional)");
        label1.setForeground(Color.gray);

        //---- label2 ----
        label2.setText("(optional)");
        label2.setForeground(Color.gray);

        //---- label3 ----
        label3.setText("(optional)");
        label3.setForeground(Color.gray);

        //---- label4 ----
        label4.setText("(optional)");
        label4.setForeground(Color.gray);

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup()
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup()
                                        .addComponent(pubmedIdTextField, GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
                                        .addComponent(reanalysisPXAccessionTextField, GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
                                        .addComponent(otherOmicsDatasetLinkTextField, GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
                                        .addComponent(scrollPane1, GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup()
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(parentProjectLabel)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(label1))
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(pubmedIdLabel)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(label2))
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(otherOmicsDatasetLinkLabel)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(label4))
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(reanalysisPXAccessionLabel)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(label3)))
                                                .addGap(0, 248, Short.MAX_VALUE))
                                        .addComponent(parentProjectDescLabel, GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup()
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(parentProjectLabel)
                                        .addComponent(label1))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(parentProjectDescLabel, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrollPane1, GroupLayout.PREFERRED_SIZE, 174, GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup()
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(label2)
                                                .addGap(3, 3, 3)
                                                .addComponent(pubmedIdTextField, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
                                        .addComponent(pubmedIdLabel))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup()
                                        .addComponent(reanalysisPXAccessionLabel)
                                        .addComponent(label3))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(reanalysisPXAccessionTextField, GroupLayout.PREFERRED_SIZE, 37, GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(otherOmicsDatasetLinkLabel)
                                        .addComponent(label4))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(otherOmicsDatasetLinkTextField, GroupLayout.PREFERRED_SIZE, 38, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JLabel parentProjectLabel;
    private JScrollPane scrollPane1;
    private JTable parentProjectTable;
    private JLabel pubmedIdLabel;
    private JTextField pubmedIdTextField;
    private JLabel reanalysisPXAccessionLabel;
    private JTextField reanalysisPXAccessionTextField;
    private JLabel otherOmicsDatasetLinkLabel;
    private JTextField otherOmicsDatasetLinkTextField;
    private JLabel label1;
    private JLabel label2;
    private JLabel label3;
    private JLabel label4;
    private JLabel parentProjectDescLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
