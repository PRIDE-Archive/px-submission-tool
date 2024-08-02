package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
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
    protected final float DEFAULT_TITLE_FONT_SIZE = 15f;

    protected JCheckBox jCheckBox = new JCheckBox();

    // use GridLayout with 2 rows and 1 column
    JPanel centerPanel = new JPanel(new GridLayout(2, 1));


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

        this.add(centerPanel, BorderLayout.CENTER);

        // set up accept license checkbox
        initDatasetLicenseAcceptCheckbox();
    }


    /**
     * Initialize experiment details panel
     */
    protected void initSubmissionSummaryDescPanel() {
        // summary description item panel
        JPanel summaryItemPanel = new SummaryItemPanel();
        summaryItemPanel.setBorder(BorderUtil.createLoweredBorder());

        this.add(summaryItemPanel, BorderLayout.NORTH);
    }


    /**
     * Initialize submission summary table
     */
    protected void initSubmissionSummaryTable() {

        // new file top panel
        JPanel newFileTopPanel = new JPanel(new BorderLayout());
        JTable summaryTable;

        // New file label
        JLabel newFileLabel = new JLabel(appContext.getProperty("resubmission.new.files.label.title"));
        newFileTopPanel.setFont(newFileTopPanel.getFont().deriveFont(DEFAULT_TITLE_FONT_SIZE));
        newFileTopPanel.add(newFileLabel, BorderLayout.NORTH);

        summaryTable = TableFactory.createSubmissionSummaryTable();

        JScrollPane scrollPane = new JScrollPane(summaryTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel jPanel = new JPanel();
        JLabel label = new JLabel("Select Upload Method:");
        // Create the checkboxes
        JCheckBox checkBox1 = new JCheckBox("FTP");
        JCheckBox checkBox2 = new JCheckBox("ASPERA");
        checkBox2.setSelected(true);
        appContext.setUploadMethod(UploadMethod.ASPERA);

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

        newFileTopPanel.add(scrollPane, BorderLayout.CENTER);
        newFileTopPanel.add(jPanel, BorderLayout.SOUTH);

        centerPanel.add(newFileTopPanel);
    }

    public void initDatasetLicenseAcceptCheckbox() {

        Navigator navigator = ((App) App.getInstance()).getNavigator();
        JButton nextButton = navigator.getNextButton();

        jCheckBox.addItemListener(e -> {
            nextButton.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
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
        this.add(jPanel, BorderLayout.SOUTH);
    }

    public JCheckBox getJCheckBox() {
        return jCheckBox;
    }
}
