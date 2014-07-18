package uk.ac.ebi.pride.gui.form;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.blocker.DefaultGUIBlocker;
import uk.ac.ebi.pride.gui.blocker.GUIBlocker;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareNavigationPanelDescriptor;
import uk.ac.ebi.pride.gui.task.CheckForUpdateTask;
import uk.ac.ebi.pride.gui.task.Task;
import uk.ac.ebi.pride.gui.task.TaskEvent;
import uk.ac.ebi.pride.gui.task.TaskListener;
import uk.ac.ebi.pride.gui.util.SubmissionRecordSerializer;
import uk.ac.ebi.pride.gui.util.UpdateChecker;
import uk.ac.ebi.pride.archive.dataprovider.project.SubmissionType;

import javax.help.HelpBroker;
import javax.swing.*;
import java.io.IOException;
import java.util.List;

/**
 * Navigation descriptor for welcome form
 *
 * @author Rui Wang
 * @version $Id$
 */
public class WelcomeDescriptor extends ContextAwareNavigationPanelDescriptor implements TaskListener<Boolean, Void> {
    private static final Logger logger = LoggerFactory.getLogger(WelcomeDescriptor.class);

    private boolean updateChecked = false;

    public WelcomeDescriptor(String id, String title, String desc) {
        super(id, title, desc, new WelcomeForm());
    }

    @Override
    public void getHelp() {
        HelpBroker hb = appContext.getMainHelpBroker();
        hb.showID("help.welcome", "javax.help.SecondaryWindow", "main");
    }

    @Override
    public void displayingPanel() {
        if (!updateChecked) {
            Task newTask = new CheckForUpdateTask();
            newTask.addTaskListener(this);
            // set task's gui blocker
            newTask.setGUIBlocker(new DefaultGUIBlocker(newTask, GUIBlocker.Scope.NONE, null));
            // add task listeners
            appContext.addTask(newTask);
        }

        firePropertyChange(DISPLAYING_PANEL_PROPERTY, false, true);
    }

    @Override
    public void beforeHidingForNextPanel() {
        // detect previous submission record
        boolean loadPreviousSubmission = detectPreviousSubmission();

        // detect submission type, show warning if it is not a full submission
        if (loadPreviousSubmission || confirmSubmissionOption()) {
            firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, false, true);
        } else {
            firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, true, false);
        }
    }

    /**
     * Confirm with submitters that they are going to perform a non-full submission
     *
     * @return true means continue
     */
    private boolean confirmSubmissionOption() {
        boolean confirmSubmissionOption = true;
        SubmissionType submissionType = appContext.getSubmissionType();

        if (!submissionType.equals(SubmissionType.COMPLETE)) {
            int n = JOptionPane.showConfirmDialog(app.getMainFrame(),
                    appContext.getProperty("unsupported.result.file.dialog.message"),
                    appContext.getProperty("unsupported.result.file.dialog.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (n != 0) {
                confirmSubmissionOption = false;
            }
        }

        return confirmSubmissionOption;
    }

    /**
     * Detect and load previously incomplete submission
     *
     * @return true means previous submission loaded
     */
    private boolean detectPreviousSubmission() {
        boolean loadPreviousSubmission = false;

        if (SubmissionRecordSerializer.hasSubmissionRecord()) {

            int n = JOptionPane.showConfirmDialog(app.getMainFrame(),
                    appContext.getProperty("incomplete.submission.record.dialog.message"),
                    appContext.getProperty("incomplete.submission.record.dialog.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (n == 0) {
                try {
                    SubmissionRecord submissionRecord = SubmissionRecordSerializer.deserialize();
                    submissionRecord.setSummaryFileUploaded(false);
                    appContext.setSubmissionRecord(submissionRecord);
                    loadPreviousSubmission = true;
                } catch (IOException e) {
                    logger.error("Failed to parse submission record at the user space");
                } catch (ClassNotFoundException e) {
                    logger.error("Failed to deserialize submission record");
                }
            }
            SubmissionRecordSerializer.remove();
        }

        return loadPreviousSubmission;
    }

    @Override
    public void succeed(TaskEvent<Boolean> booleanTaskEvent) {
        Boolean hasUpdate = booleanTaskEvent.getValue();
        if (hasUpdate) {
            UpdateChecker.showUpdateDialog();
        }
        updateChecked = true;
    }

    @Override
    public void started(TaskEvent<Void> event) {
    }

    @Override
    public void process(TaskEvent<List<Void>> listTaskEvent) {
    }

    @Override
    public void finished(TaskEvent<Void> event) {
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
