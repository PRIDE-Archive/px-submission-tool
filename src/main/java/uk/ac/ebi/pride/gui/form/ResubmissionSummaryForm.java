package uk.ac.ebi.pride.gui.form;

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

        existingFileTopPanel.add(scrollPane,  BorderLayout.CENTER);
        centerPanel.add(existingFileTopPanel);
    }

    public JCheckBox getJCheckBox() {
        return jCheckBox;
    }
}
