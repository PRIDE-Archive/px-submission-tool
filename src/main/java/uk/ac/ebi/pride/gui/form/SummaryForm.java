package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.gui.form.panel.SummaryItemPanel;
import uk.ac.ebi.pride.gui.form.table.TableFactory;
import uk.ac.ebi.pride.gui.navigation.Navigator;
import uk.ac.ebi.pride.gui.util.BorderUtil;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ItemEvent;

/**
 * SummaryForm shows a summary of the submission
 *
 * @author Rui Wang
 * @version $Id$
 */
public class SummaryForm extends Form {
    private JTable summaryTable;

    private JCheckBox jCheckBox = new JCheckBox();

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

        // set up accept license checkbox
        initDatasetLicenseAcceptCheckbox();
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

    public void initDatasetLicenseAcceptCheckbox() {

        Navigator navigator = ((App) App.getInstance()).getNavigator();
        JButton nextButton = navigator.getNextButton();

        jCheckBox.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED){
                nextButton.setEnabled(true);
            }else {
                nextButton.setEnabled(false);
            }
        });

        // html content
        JEditorPane editorPane = new JEditorPane("text/html", appContext.getProperty("summary.dataset.accept.license.text"));
        editorPane.addHyperlinkListener(
                e -> {
                    try {
                        if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
                            Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        // couldn't display error message
                    }
                });
        editorPane.setEditable(false);
        editorPane.setBackground(new JLabel().getBackground());

        JPanel jPanel = new JPanel();
        jPanel.add(jCheckBox);
        jPanel.add(editorPane);
        this.add(jPanel,BorderLayout.SOUTH);
    }

    public JCheckBox getJCheckBox() {
        return jCheckBox;
    }
}
