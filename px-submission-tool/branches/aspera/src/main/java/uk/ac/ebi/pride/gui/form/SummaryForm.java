package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.gui.form.panel.SummaryItemPanel;
import uk.ac.ebi.pride.gui.form.table.TableFactory;
import uk.ac.ebi.pride.gui.util.BorderUtil;

import javax.swing.*;
import java.awt.*;

/**
 * SummaryForm shows a summary of the submission
 *
 * @author Rui Wang
 * @version $Id$
 */
public class SummaryForm extends Form{
    private JTable summaryTable;

    public SummaryForm() {
        initComponents();
    }

    private void initComponents() {
        // setup the main pane
        this.setLayout(new BorderLayout());
        
        // setup submission summary description panel
        initSubmissionSummaryDescPanel();
        
        // setup submission summary table
        initSubmissionSummaryTable();
    }

    /**
     * Initialize experiment details panel
     */
    private void initSubmissionSummaryDescPanel() {
        // summary description item panel
        JPanel summaryItemPanel = new SummaryItemPanel();
        summaryItemPanel.setBorder(BorderUtil.createLoweredBorder());

        this.add(summaryItemPanel, BorderLayout.NORTH);
    }



    /**
     * Initialize submission summary table
     */
    private void initSubmissionSummaryTable() {
        summaryTable = TableFactory.createSubmissionSummaryTable();
        
        JScrollPane scrollPane = new JScrollPane(summaryTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        this.add(scrollPane, BorderLayout.CENTER);
    }

}
