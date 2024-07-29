package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
import uk.ac.ebi.pride.gui.form.table.TableFactory;

import javax.swing.*;
import java.awt.*;

/**
 * SummaryForm shows a summary of the submission
 *
 * @author Rui Wang
 * @version $Id$
 */
public class ResubmissionSummaryForm extends SummaryForm {

    public ResubmissionSummaryForm() {
        initComponents();
    }

    private void initComponents() {
        initResubmissionModTable();
        this.add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Initialize submission summary table
     */
    private void initResubmissionModTable() {

        // existing file top panel
        JPanel existingFileTopPanel = new JPanel(new BorderLayout());
        JTable resubmissionModifiedTable;

        // existing file label
        JLabel existingFileLabel = new JLabel(appContext.getProperty("resubmission.existing.files.label.title"));
        existingFileLabel.setFont(existingFileLabel.getFont().deriveFont(DEFAULT_TITLE_FONT_SIZE));
        existingFileTopPanel.add(existingFileLabel, BorderLayout.NORTH);

        resubmissionModifiedTable = TableFactory.createResubmissionSummaryTable();
        resubmissionModifiedTable.setShowGrid(false);
        resubmissionModifiedTable.setShowHorizontalLines(false);
        resubmissionModifiedTable.setShowVerticalLines(false);

        JScrollPane scrollPane = new JScrollPane(resubmissionModifiedTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel jPanel = new JPanel();
        JLabel label = new JLabel("Select Upload Method:");
        // Create the checkboxes
        JCheckBox checkBox1 = new JCheckBox("FTP");
        JCheckBox checkBox2 = new JCheckBox("ASPERA");
        checkBox2.setSelected(true);

        // Create a ButtonGroup to make the checkboxes mutually exclusive
        ButtonGroup group = new ButtonGroup();
        group.add(checkBox1);
        group.add(checkBox2);

        // Add ActionListener to the checkboxes
        checkBox1.addActionListener(e -> appContext.setUploadMethod(UploadMethod.FTP));
        checkBox2.addActionListener(e -> appContext.setUploadMethod(UploadMethod.ASPERA));

        // Add the checkboxes to the panel
        jPanel.add(label);
        jPanel.add(checkBox1);
        jPanel.add(checkBox2);

        existingFileTopPanel.add(scrollPane,  BorderLayout.CENTER);
        existingFileTopPanel.add(jPanel,BorderLayout.SOUTH);
        centerPanel.add(existingFileTopPanel);
    }

    public JCheckBox getJCheckBox() {
        return jCheckBox;
    }
}
