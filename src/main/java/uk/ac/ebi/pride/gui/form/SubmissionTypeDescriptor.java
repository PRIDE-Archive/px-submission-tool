package uk.ac.ebi.pride.gui.form;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.dataprovider.utils.SubmissionTypeConstants;
import uk.ac.ebi.pride.archive.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.data.model.ProjectMetaData;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareNavigationPanelDescriptor;
import uk.ac.ebi.pride.gui.task.CheckForUpdateTask;
import uk.ac.ebi.pride.gui.util.SubmissionRecordSerializer;
import uk.ac.ebi.pride.gui.util.UpdateChecker;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.toolsuite.gui.blocker.DefaultGUIBlocker;
import uk.ac.ebi.pride.toolsuite.gui.blocker.GUIBlocker;
import uk.ac.ebi.pride.toolsuite.gui.task.Task;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskEvent;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskListener;
import uk.ac.ebi.pride.gui.util.SubmissionRecordSerializer;
import uk.ac.ebi.pride.gui.util.UpdateChecker;


import javax.help.HelpBroker;
import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Navigation descriptor for SubmissionTypeConstants form
 *
 * @author Rui Wang
 * @version $Id$
 */
public class SubmissionTypeDescriptor extends ContextAwareNavigationPanelDescriptor implements TaskListener<Boolean, Void> {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionTypeDescriptor.class);

    private boolean updateChecked = false;
    private SubmissionTypeForm submissionTypeForm = null;

    public SubmissionTypeDescriptor(String id, String title, String desc) {
        super(id, title, desc, new SubmissionTypeForm());
        submissionTypeForm = (SubmissionTypeForm) getNavigationPanel();
        submissionTypeForm.getPropertyChangeHelper().addPropertyChangeListener(new TrainingModeListener());
    }

    @Override
    public void getHelp() {
        HelpBroker hb = appContext.getMainHelpBroker();
        hb.showID("help.submission.type", "javax.help.SecondaryWindow", "main");
    }

    @Override
    public void displayingPanel() {
        if (!updateChecked) {
            // TODO - is this working?
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

        ProjectMetaData projectMetaData = appContext.getSubmissionRecord().getSubmission().getProjectMetaData();
        projectMetaData.clearMassSpecExperimentMethods();

        if (!projectMetaData.isResubmission()) {
            appContext.setResubmission(false);
        }

        if (appContext.isResubmission() && submissionTypeForm.getResubmissionChangeTypeError().length() > 1) {
            JOptionPane.showMessageDialog(app.getMainFrame(),
                    submissionTypeForm.getResubmissionChangeTypeError(),
                    appContext.getProperty("resubmission.submission.type.change.prohibit"),
                    JOptionPane.WARNING_MESSAGE,
                    GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("icon.warning.normal.small")));
            firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, true, false);
        } else if (loadPreviousSubmission || confirmSubmissionOption()) { // detect submission type, show warning if it is not a full submission
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
        SubmissionTypeConstants submissionType = appContext.getSubmissionType();

        if (submissionType.equals(SubmissionTypeConstants.PARTIAL)) {
            int n = JOptionPane.showConfirmDialog(app.getMainFrame(),
                    appContext.getProperty("unsupported.result.file.dialog.message"),
                    appContext.getProperty("unsupported.result.file.dialog.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("icon.warning.normal.small")));
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

                    String resubmissionAccession = submissionRecord.getSubmission().getProjectMetaData().getResubmissionPxAccession();
                    if(resubmissionAccession!=null && !resubmissionAccession.equals("")){
                        appContext.setResubmission(true);
                    } else {
                        appContext.setResubmission(false);
                    }
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

    private class TrainingModeListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String propName = evt.getPropertyName();
            logger.debug("Property change event: " + propName);
            if (SubmissionTypeForm.PropertyChangeBroadcaster.TRAINING_MODE_TOGGLE.equals(propName)) {
                firePropertyChange(TRAINING_MODE_TOGGLE_PROPERTY, evt.getOldValue(), evt.getNewValue());
            }
        }
    }
}
