package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.validation.SubmissionValidator;
import uk.ac.ebi.pride.data.validation.ValidationMessage;
import uk.ac.ebi.pride.data.validation.ValidationReport;
import uk.ac.ebi.pride.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.form.table.TableFactory;
import uk.ac.ebi.pride.gui.form.table.model.ResultFileTableModel;
import uk.ac.ebi.pride.gui.form.table.model.SampleMetaDataTableModel;
import uk.ac.ebi.pride.gui.util.BalloonTipUtil;
import uk.ac.ebi.pride.gui.util.ValidationState;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Form component to collection sample metadata for each result file
 *
 * @author Rui Wang
 * @version $Id$
 */
public class SampleMetaDataForm extends Form {
    public static final float DEFAULT_TITLE_FONT_SIZE = 15f;

    /**
     * Result file table
     */
    private JTable resultFileTable;

    /**
     * Result file table model
     */
    private ResultFileTableModel resultFileTableModel;
    /**
     * Mapping file table
     */
    private JTable sampleMetaDataTable;

    /**
     * Mapping file table model
     */
    private SampleMetaDataTableModel sampleMetaDataTableModel;

    public SampleMetaDataForm() {
        initComponents();
    }

    private void initComponents() {
        // setup the main panel
        this.setLayout(new GridLayout(2, 1, 0, 20));

        // result file panel
        JPanel resultFilePanel = new JPanel(new BorderLayout());

        // result file top panel
        JPanel resultFileTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // result file label
        JLabel resultFileLabel = new JLabel(appContext.getProperty("sample.metadata.result.file.label.title"));
        resultFileLabel.setFont(resultFileLabel.getFont().deriveFont(DEFAULT_TITLE_FONT_SIZE));
        JLabel resultFileDescLabel = new JLabel(appContext.getProperty("sample.metadata.result.file.description.title"));
        resultFileDescLabel.setForeground(Color.gray);

        resultFileTopPanel.add(resultFileLabel);
        resultFileTopPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        resultFileTopPanel.add(resultFileDescLabel);
        resultFilePanel.add(resultFileTopPanel, BorderLayout.NORTH);

        // result file table
        resultFileTable = TableFactory.createResultFileTable();

        // result file table model
        resultFileTableModel = (ResultFileTableModel) resultFileTable.getModel();

        // register selection listener
        ListSelectionModel selectionModel = resultFileTable.getSelectionModel();
        selectionModel.addListSelectionListener(new ResultFileSelectionListener());

        // scroll pane
        JScrollPane resultFileScrollPane = new JScrollPane(resultFileTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        resultFilePanel.add(resultFileScrollPane, BorderLayout.CENTER);

        this.add(resultFilePanel);

        // sample metadata panel
        JPanel sampleMetaDataPanel = new JPanel(new BorderLayout());

        // sample metadata top panel
        JPanel sampleMetaDataTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));


        // sample metadata label
        JLabel sampleMetaDataLabel = new JLabel(appContext.getProperty("sample.metadata.label.title"));
        sampleMetaDataLabel.setFont(resultFileLabel.getFont().deriveFont(DEFAULT_TITLE_FONT_SIZE));
        JLabel sampleMetaDataDescLabel = new JLabel(appContext.getProperty("sample.metadata.description.title"));
        sampleMetaDataDescLabel.setForeground(Color.gray);

        sampleMetaDataTopPanel.add(sampleMetaDataLabel);
        sampleMetaDataTopPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        sampleMetaDataTopPanel.add(sampleMetaDataDescLabel);
        sampleMetaDataPanel.add(sampleMetaDataTopPanel, BorderLayout.NORTH);

        // mapping file table
        sampleMetaDataTable = TableFactory.createSampleMetaDataTable();

        // mapping file table model
        sampleMetaDataTableModel = (SampleMetaDataTableModel) sampleMetaDataTable.getModel();

        //scroll pane
        JScrollPane mappingFileScrollPane = new JScrollPane(sampleMetaDataTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sampleMetaDataPanel.add(mappingFileScrollPane, BorderLayout.CENTER);

        this.add(sampleMetaDataPanel);
    }

    @Override
    public ValidationState doValidation() {
        boolean valid = true;
        int cnt = 0;
        java.util.List<DataFile> resultFiles = appContext.getSubmissionFilesByType(ProjectFileType.RESULT);
        ArrayList<String> errorMessages = new ArrayList<>();
        for (DataFile resultFile : resultFiles) {
            ValidationReport validationReport = SubmissionValidator.validateSampleMetaDataEntry(resultFile, true);
            if (validationReport.hasError()) {
                valid = false;
                cnt++;
                for (ValidationMessage message : validationReport.getMessages()) {
                    if (message.getType().equals(ValidationMessage.Type.ERROR)) {
                        errorMessages.add(message.getMessage());
                    }
                }
            }
        }
        if (valid) {
            return ValidationState.SUCCESS;
        } else {  // show balloon warning tip
            if (warningBalloonTip != null) {
                warningBalloonTip.closeBalloon(); // close previous balloon tip
            }
            JLabel newWarningContents = new JLabel("<html>" + "<b>Please ensure all result files have complete experimental details:</b><br/>"
                + "<li>" + cnt + " result files have incomplete details" + "</li>" + "</b><br/>" + Arrays.toString(errorMessages.toArray()) + "</html>");
            newWarningContents.setIcon(GUIUtilities.loadIcon(appContext.getProperty("warning.message.icon")));
            warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(resultFileTable, newWarningContents);
            showWarnings();
            return ValidationState.ERROR;
        }
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


    /**
     * Get result file table
     *
     * @return JTable  result file table
     */
    public JTable getResultFileTable() {
        return resultFileTable;
    }

    /**
     * Get mapping file table
     *
     * @return JTable  mapping file table
     */
    public JTable getSampleMetaDataTable() {
        return sampleMetaDataTable;
    }

    /**
     * Listen to the row selection on the result file table
     */
    private class ResultFileSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                int rowNum = resultFileTable.getSelectedRow();
                int rowCnt = resultFileTable.getRowCount();
                if (rowCnt > 0 && rowNum >= 0) {
                    // get table model
                    // fire a property change event with selected identification id
                    int columnNum = resultFileTableModel.getColumnIndex(ResultFileTableModel.TableHeader.ANNOTATION.getHeader());

                    DataFile selectedResultFile = (DataFile) resultFileTableModel.getValueAt(resultFileTable.convertRowIndexToModel(rowNum), columnNum);

                    sampleMetaDataTableModel.setDataFile(selectedResultFile);
                }
            }
        }
    }
}
