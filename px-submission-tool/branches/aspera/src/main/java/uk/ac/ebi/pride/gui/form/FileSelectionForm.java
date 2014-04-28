package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.form.action.AddFileSelectionAction;
import uk.ac.ebi.pride.gui.form.comp.GradientPanel;
import uk.ac.ebi.pride.gui.form.dialog.FileSelectionValidationErrorDialog;
import uk.ac.ebi.pride.gui.form.table.TableFactory;
import uk.ac.ebi.pride.gui.task.TaskEvent;
import uk.ac.ebi.pride.gui.task.TaskListener;
import uk.ac.ebi.pride.gui.util.BalloonTipUtil;
import uk.ac.ebi.pride.gui.util.BorderUtil;
import uk.ac.ebi.pride.gui.util.DataFileValidationMessage;
import uk.ac.ebi.pride.gui.util.ValidationState;
import uk.ac.ebi.pride.prider.dataprovider.project.SubmissionType;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * This form is responsible for file selection or loading pre-existing submission file
 *
 * @author Rui Wang
 * @version $Id$
 */
public class FileSelectionForm extends Form implements PropertyChangeListener, TaskListener<DataFileValidationMessage, Void> {
    private static final float DEFAULT_BUTTON_FONT_SIZE = 14f;
    private static final int DEFAULT_BUTTON_WIDTH = 120;
    private static final int DEFAULT_BUTTON_HEIGHT = 40;

    private static enum RequirementStatus {
        REQUIRED, OPTIONAL, REQUIRED_AND_OPTIONAL
    }

    /**
     * file selection table
     */
    private JTable fileSelectionTable;

    private JPanel notePanel;

    public FileSelectionForm() {
        initComponents();
    }

    private void initComponents() {
        // setup main pane
        this.setLayout(new BorderLayout());

        // get app context
        appContext = (AppContext) App.getInstance().getDesktopContext();
        appContext.addPropertyChangeListener(this);

        // button pane
        JPanel buttonPane = new JPanel(new BorderLayout());
        buttonPane.setBorder(BorderUtil.createLoweredBorder());

        // left button pane
        JPanel leftButtonPane = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // add file button
        JButton addFileButton = new JButton(new AddFileSelectionAction());
        addFileButton.setFont(addFileButton.getFont().deriveFont(DEFAULT_BUTTON_FONT_SIZE));
        addFileButton.setPreferredSize(new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        leftButtonPane.add(addFileButton);

        // add url button
//        JButton addURLButton = new JButton(new AddURLAction());
//        addURLButton.setFont(addFileButton.getFont().deriveFont(DEFAULT_BUTTON_FONT_SIZE));
//        addURLButton.setPreferredSize(new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
//        leftButtonPane.add(addURLButton);

        buttonPane.add(leftButtonPane, BorderLayout.WEST);

        // load submission file panel
        JPanel loadSubmissionFilePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        buttonPane.add(loadSubmissionFilePanel, BorderLayout.EAST);
        this.add(buttonPane, BorderLayout.NORTH);

        // file selection table
        fileSelectionTable = TableFactory.createFileSelectionTable();

        // scroll pane
        JScrollPane scrollPane = new JScrollPane(fileSelectionTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.add(scrollPane, BorderLayout.CENTER);

        // balloon note
        SubmissionType submissionType = appContext.getSubmissionType();
        notePanel = createNotePane(submissionType == null ? SubmissionType.COMPLETE : submissionType);

        this.add(notePanel, BorderLayout.SOUTH);

    }


    private JPanel createNotePane(SubmissionType submissionType) {
        JPanel notePanel = new JPanel(new BorderLayout());
        notePanel.add(Box.createRigidArea(new Dimension(5, 5)), BorderLayout.NORTH);

        JPanel borderPanel = new JPanel(new BorderLayout());
//        borderPanel.setBorder(BorderUtil.createLoweredBorder());
        notePanel.add(borderPanel, BorderLayout.CENTER);

        // title
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel(appContext.getProperty("file.category.note.title"));
        titleLabel.setFont(titlePanel.getFont().deriveFont(14f).deriveFont(Font.BOLD));
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        borderPanel.add(titlePanel, BorderLayout.NORTH);

        JPanel contentLayoutPanel = new JPanel();
        GridLayout gridLayout = new GridLayout(4, 2);
        gridLayout.setVgap(2);
        contentLayoutPanel.setLayout(gridLayout);
        contentLayoutPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        // result file type
        boolean isCompleteSubmission = submissionType.equals(SubmissionType.COMPLETE);
        if (isCompleteSubmission) {
            JPanel resultTypeItemPanel = createNoteItemPanel(appContext.getProperty("result.file.type.note"), RequirementStatus.REQUIRED);
            contentLayoutPanel.add(resultTypeItemPanel);
        }

        // raw file type
        JPanel rawTypeItemPanel = createNoteItemPanel(appContext.getProperty("raw.file.type.note"), RequirementStatus.REQUIRED);
        contentLayoutPanel.add(rawTypeItemPanel);

        // search file type
        JPanel searchTypeItemPanel = createNoteItemPanel(appContext.getProperty("search.file.type.optional.note"), isCompleteSubmission ? RequirementStatus.OPTIONAL : RequirementStatus.REQUIRED);
        contentLayoutPanel.add(searchTypeItemPanel);

        // peak file type
        JPanel peakTypeItemPanel = createNoteItemPanel(appContext.getProperty("peak.file.type.note"), isCompleteSubmission ? RequirementStatus.REQUIRED_AND_OPTIONAL : RequirementStatus.OPTIONAL);
        contentLayoutPanel.add(peakTypeItemPanel);

        // quantification file type
        JPanel quantTypeItemPanel = createNoteItemPanel(appContext.getProperty("quant.file.type.note"), RequirementStatus.OPTIONAL);
        contentLayoutPanel.add(quantTypeItemPanel);

        // gel file type
        JPanel gelTypeItemPanel = createNoteItemPanel(appContext.getProperty("gel.file.type.note"), RequirementStatus.OPTIONAL);
        contentLayoutPanel.add(gelTypeItemPanel);

        // other file type
        JPanel otherTypeItemPanel = createNoteItemPanel(appContext.getProperty("other.file.type.note"), RequirementStatus.OPTIONAL);
        contentLayoutPanel.add(otherTypeItemPanel);

        borderPanel.add(contentLayoutPanel, BorderLayout.CENTER);

        return notePanel;
    }

    private JPanel createNoteItemPanel(String message, RequirementStatus status) {
        JPanel noteItemPanel = new GradientPanel();
        noteItemPanel.setLayout(new BorderLayout());
        noteItemPanel.setBorder(BorderFactory.createLineBorder(Color.gray));

        JPanel entryPanel = new JPanel();
        entryPanel.setPreferredSize(new Dimension(4, 10));
        switch (status) {
            case REQUIRED:
                entryPanel.setBackground(new Color(35, 166, 76));
                break;
            case OPTIONAL:
                entryPanel.setBackground(new Color(245, 199, 61));
                break;
            case REQUIRED_AND_OPTIONAL:
                entryPanel.setBackground(new Color(109, 226, 232));
                break;
        }
        noteItemPanel.add(entryPanel, BorderLayout.WEST);

        JPanel innerBorderPanel = new JPanel(new BorderLayout());
        innerBorderPanel.setOpaque(false);
        innerBorderPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));

        JLabel noteLabel = new JLabel(message);
        noteLabel.setFont(noteLabel.getFont().deriveFont(12f));
        innerBorderPanel.add(noteLabel, BorderLayout.CENTER);
        noteItemPanel.add(innerBorderPanel, BorderLayout.CENTER);

        return noteItemPanel;
    }

    @Override
    public ValidationState doValidation() {
        // external validation, see ValidateFileSelection
        return null;
    }

    /**
     * Show the warning balloon tip
     *
     * @param errMsg error message
     */
    private void showWarnings(String errMsg) {
        hideWarnings();

        // Create the balloon tip
        JLabel newWarningContents = new JLabel(errMsg);
        newWarningContents.setIcon(GUIUtilities.loadIcon(appContext.getProperty("warning.message.icon")));

        warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(fileSelectionTable, newWarningContents);
        warningBalloonTip.setVisible(true);

        this.revalidate();
        this.repaint();
    }

    /**
     * Get the warning balloon tip
     */
    public void hideWarnings() {
        if (warningBalloonTip != null && warningBalloonTip.isVisible()) {
            warningBalloonTip.closeBalloon();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();
        if (propName.equals(AppContext.SUBMISSION_TYPE_CHANGED) || propName.equals(AppContext.NEW_SUBMISSION_FILE)) {
            // repaint note
            this.remove(notePanel);
            notePanel = createNotePane(appContext.getSubmissionType());
            this.add(notePanel, BorderLayout.SOUTH);
            this.revalidate();
            this.repaint();
        }
    }

    @Override
    public void started(TaskEvent<Void> event) {
        // clear previous warning
        if (warningBalloonTip != null) {
            warningBalloonTip.closeBalloon();
        }
    }

    @Override
    public void succeed(TaskEvent<DataFileValidationMessage> event) {
        DataFileValidationMessage message = event.getValue();

        if (message.getState().equals(ValidationState.ERROR)) {
            if (message.getDataFileValidationResults().isEmpty()) {
                showWarnings(message.getMessage());
            } else if (!message.getDataFileValidationResults().isEmpty()) {
                FileSelectionValidationErrorDialog errorDialog = new FileSelectionValidationErrorDialog(((App) App.getInstance()).getMainFrame(), message);
                errorDialog.setLocationRelativeTo(app.getMainFrame());
                errorDialog.setVisible(true);
            }
        }

    }

    @Override
    public void process(TaskEvent<List<Void>> event) {
    }

    @Override
    public void finished(TaskEvent<Void> event) {
        // update table as the file type might change as the result of the validation
        fileSelectionTable.revalidate();
        fileSelectionTable.repaint();
    }

    @Override
    public void failed(TaskEvent<Throwable> event) {
    }

    @Override
    public void cancelled(TaskEvent<Void> event) {
    }

    @Override
    public void interrupted(TaskEvent<InterruptedException> iex) {
    }

    @Override
    public void progress(TaskEvent<Integer> progress) {
    }
}
