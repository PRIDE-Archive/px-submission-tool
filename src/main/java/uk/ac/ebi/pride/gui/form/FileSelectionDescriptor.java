package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.dataprovider.utils.SubmissionTypeConstants;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.gui.form.table.TableFactory;
import uk.ac.ebi.pride.toolsuite.gui.blocker.DefaultGUIBlocker;
import uk.ac.ebi.pride.toolsuite.gui.blocker.GUIBlocker;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareNavigationPanelDescriptor;
import uk.ac.ebi.pride.gui.form.dialog.TaskDialog;
import uk.ac.ebi.pride.gui.task.FileScanAndValidationTask;
import uk.ac.ebi.pride.toolsuite.gui.task.Task;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskEvent;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskListener;
import uk.ac.ebi.pride.gui.util.ChecksumSubmissionValidator;
import uk.ac.ebi.pride.gui.util.Constant;
import uk.ac.ebi.pride.gui.util.DataFileValidationMessage;
import uk.ac.ebi.pride.gui.util.ValidationState;

import javax.help.HelpBroker;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Navigation descriptor for file selection form
 *
 * @author Rui Wang
 * @version $Id$
 */
public class FileSelectionDescriptor extends ContextAwareNavigationPanelDescriptor implements TaskListener<DataFileValidationMessage, Void> {

    public FileSelectionDescriptor(String id, String title, String desc) {
        super(id, title, desc, new FileSelectionForm());
    }

    @Override
    public void getHelp() {
        HelpBroker hb = appContext.getMainHelpBroker();
        hb.showID("help.file.selection", "javax.help.SecondaryWindow", "main");
    }

    @Override
    public boolean toSkipPanel() {
        final String resubmissionPxAccession = appContext.getSubmissionRecord().getSubmission().getProjectMetaData().getResubmissionPxAccession();
        return resubmissionPxAccession != null && resubmissionPxAccession.length()>0;
    }

    @Override
    public void beforeHidingForNextPanel() {
        // validate the content in the table
        FileSelectionForm form = (FileSelectionForm) getNavigationPanel();
        Submission submission = appContext.getSubmissionRecord().getSubmission();



        Task fileSelectionValidationTask = new FileScanAndValidationTask(submission);
        fileSelectionValidationTask.addTaskListener(form);
        fileSelectionValidationTask.addTaskListener(this);
        TaskDialog<DataFileValidationMessage, Void> taskDialog = new TaskDialog<>(((App) App.getInstance()).getMainFrame(),
                appContext.getProperty("file.selection.validation.dialog.title"),
                appContext.getProperty("file.selection.validation.dialog.message"),
                false);
        taskDialog.setLocationRelativeTo(app.getMainFrame());
        taskDialog.setVisible(true);
        fileSelectionValidationTask.addTaskListener(taskDialog);
        fileSelectionValidationTask.addOwner(FileSelectionDescriptor.this);
        fileSelectionValidationTask.setGUIBlocker(new DefaultGUIBlocker(fileSelectionValidationTask, GUIBlocker.Scope.NONE, null));

        appContext.addTask(fileSelectionValidationTask);
    }

    @Override
    public void beforeHidingForPreviousPanel() {
        //hide any visible warning balloon tip
        FileSelectionForm form = (FileSelectionForm) getNavigationPanel();
        form.hideWarnings();
        firePropertyChange(BEFORE_HIDING_FOR_PREVIOUS_PANEL_PROPERTY, false, true);
    }

    @Override
    public void beforeDisplayingPanel() {
        FileSelectionForm form = (FileSelectionForm) getNavigationPanel();
        JTable fileSelectionTable = TableFactory.createFileSelectionTable(appContext.getSubmissionType());
        form.remove(form.getScrollPane());
        form.setFileSelectionTable(fileSelectionTable);
        JScrollPane newScrollPane = new JScrollPane(fileSelectionTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        form.setScrollPane(newScrollPane);
        form.add(newScrollPane,BorderLayout.CENTER);
        firePropertyChange(BEFORE_DISPLAY_PANEL_PROPERTY, false, true);
    }

    @Override
    public void started(TaskEvent<Void> event) {
    }

    @Override
    public void process(TaskEvent<List<Void>> event) {
    }

    @Override
    public void finished(TaskEvent<Void> event) {
    }

    @Override
    public void failed(TaskEvent<Throwable> event) {
        firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, true, false);
    }

    @Override
    public void succeed(TaskEvent<DataFileValidationMessage> event) {
        DataFileValidationMessage message = event.getValue();
        if (message.getState().equals(ValidationState.SUCCESS)) {
            FileSelectionForm form = (FileSelectionForm) getNavigationPanel();
            form.hideWarnings();
            if (!validateCustomChecksumFileIfProvided()) {
                firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, true, false);
                return;
            }
            firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, false, true);
        } else {
            firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, true, false);
        }
    }

    private boolean validateCustomChecksumFileIfProvided() {
        if (!appContext.isCustomChecksumFileProvided()) {
            return true;
        }

        Submission submission = appContext.getSubmissionRecord().getSubmission();
        DataFile checksumDataFile = getChecksumDataFile(submission);
        try {
            ChecksumSubmissionValidator.Result r = ChecksumSubmissionValidator.validate(
                    submission.getDataFiles(),
                    checksumDataFile == null ? null : checksumDataFile.getFile(),
                    Constant.CHECKSUM_FILE_NAME);
            if (r.isValid()) {
                return true;
            }
            if (!r.wasContentValidated()) {
                showChecksumFileNotProvidedOrUnreadable(checksumDataFile == null ? null : checksumDataFile.getFile());
                return false;
            }

            showInvalidChecksumFileError(checksumDataFile == null ? null : checksumDataFile.getFile(), r);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    ((App) App.getInstance()).getMainFrame(),
                    "Could not read the checksum file.\n\n" + e.getMessage(),
                    "Invalid checksum file",
                    JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    private DataFile getChecksumDataFile(Submission submission) {
        for (DataFile dataFile : submission.getDataFiles()) {
            if (Constant.CHECKSUM_FILE_NAME.equals(dataFile.getFileName())) {
                return dataFile;
            }
        }
        return null;
    }

    private void showChecksumFileNotProvidedOrUnreadable(File checksumFile) {
        String message;
        if (checksumFile != null && checksumFile.exists() && !checksumFile.canRead()) {
            message = "The checksum file cannot be read.";
        } else {
            message = "The checksum file is not provided.";
        }
        JOptionPane.showMessageDialog(
                ((App) App.getInstance()).getMainFrame(),
                message,
                "Checksum file",
                JOptionPane.WARNING_MESSAGE);
    }

    private void showInvalidChecksumFileError(File checksumFile, ChecksumSubmissionValidator.Result result) {
        String name = Constant.CHECKSUM_FILE_NAME;
        StringBuilder message = new StringBuilder("The provided " + name + " is not valid for the selected files.");
        if (checksumFile != null) {
            message.append("\n\nChecksum file:\n").append(checksumFile.getAbsolutePath());
        }
        if (!result.getMissingInChecksum().isEmpty()) {
            message.append("\n\nSelected files not listed in ").append(name).append(":");
            appendLimitedLines(message, result.getMissingInChecksum());
        }
        if (!result.getExtraInChecksum().isEmpty()) {
            message.append("\n\nEntries in ").append(name).append(" that do not match any selected file:");
            appendLimitedLines(message, result.getExtraInChecksum());
        }

        JTextArea messageArea = new JTextArea(message.toString());
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setColumns(72);

        JOptionPane.showMessageDialog(
                ((App) App.getInstance()).getMainFrame(),
                messageArea,
                "Invalid checksum file",
                JOptionPane.ERROR_MESSAGE);
    }

    private void appendLimitedLines(StringBuilder message, List<String> items) {
        int limit = Math.min(items.size(), 20);
        for (int i = 0; i < limit; i++) {
            message.append("\n- ").append(items.get(i));
        }
        if (items.size() > limit) {
            message.append("\n... and ").append(items.size() - limit).append(" more");
        }
    }

    @Override
    public void cancelled(TaskEvent<Void> event) {
        firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, true, false);
    }

    @Override
    public void interrupted(TaskEvent<InterruptedException> iex) {
        firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, true, false);
    }

    @Override
    public void progress(TaskEvent<Integer> progress) {
    }
}
