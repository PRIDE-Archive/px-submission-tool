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
import uk.ac.ebi.pride.gui.util.DataFileValidationMessage;
import uk.ac.ebi.pride.gui.util.ValidationState;

import javax.help.HelpBroker;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            List<String> missingFileNames = getMissingChecksumFileNames(submission, checksumDataFile == null ? null : checksumDataFile.getFile());
            if (missingFileNames.isEmpty()) {
                return true;
            }

            showInvalidChecksumFileError(checksumDataFile == null ? null : checksumDataFile.getFile(), missingFileNames);
        } catch (IOException e) {
            showInvalidChecksumFileError(checksumDataFile == null ? null : checksumDataFile.getFile(), List.of(e.getMessage()));
        }
        return false;
    }

    private DataFile getChecksumDataFile(Submission submission) {
        String checksumFilename = appContext.getProperty("checksum.filename");
        for (DataFile dataFile : submission.getDataFiles()) {
            if (checksumFilename.equals(dataFile.getFileName())) {
                return dataFile;
            }
        }
        return null;
    }

    private List<String> getMissingChecksumFileNames(Submission submission, File checksumFile) throws IOException {
        List<String> missingFileNames = new ArrayList<>();
        if (checksumFile == null || !checksumFile.exists() || !checksumFile.canRead()) {
            for (DataFile dataFile : submission.getDataFiles()) {
                if (!isChecksumDataFile(dataFile)) {
                    missingFileNames.add(dataFile.getFileName());
                }
            }
            return missingFileNames;
        }

        Set<String> checksumEntries = readChecksumEntries(checksumFile);
        for (DataFile dataFile : submission.getDataFiles()) {
            if (!isChecksumDataFile(dataFile) && !checksumEntries.contains(dataFile.getFileName())) {
                missingFileNames.add(dataFile.getFileName());
            }
        }
        return missingFileNames;
    }

    private Set<String> readChecksumEntries(File checksumFile) throws IOException {
        Set<String> checksumEntries = new HashSet<>();
        List<String> lines = java.nio.file.Files.readAllLines(checksumFile.toPath(), Charset.defaultCharset());
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }

            String[] parts = trimmedLine.split("\\s+");
            if (parts.length > 0) {
                checksumEntries.add(parts[0]);
                checksumEntries.add(new File(parts[0]).getName());
            }
        }
        return checksumEntries;
    }

    private boolean isChecksumDataFile(DataFile dataFile) {
        return appContext.getProperty("checksum.filename").equals(dataFile.getFileName());
    }

    private void showInvalidChecksumFileError(File checksumFile, List<String> missingFileNames) {
        StringBuilder message = new StringBuilder("The provided checksum.txt is not valid for the selected files.");
        if (checksumFile != null) {
            message.append("\n\nChecksum file:\n").append(checksumFile.getAbsolutePath());
        }
        message.append("\n\nMissing file names:");
        int limit = Math.min(missingFileNames.size(), 20);
        for (int i = 0; i < limit; i++) {
            message.append("\n- ").append(missingFileNames.get(i));
        }
        if (missingFileNames.size() > limit) {
            message.append("\n... and ").append(missingFileNames.size() - limit).append(" more");
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
