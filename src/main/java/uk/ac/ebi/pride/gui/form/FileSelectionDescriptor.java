package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.data.model.Submission;
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
    public void beforeHidingForNextPanel() {
        // validate the content in the table
        FileSelectionForm form = (FileSelectionForm) getNavigationPanel();
        Submission submission = appContext.getSubmissionRecord().getSubmission();

        Task fileSelectionValidationTask = new FileScanAndValidationTask(submission);
        fileSelectionValidationTask.addTaskListener(form);
        fileSelectionValidationTask.addTaskListener(this);
        TaskDialog<DataFileValidationMessage, Void> taskDialog = new TaskDialog<DataFileValidationMessage, Void>(((App) App.getInstance()).getMainFrame(),
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
            firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, false, true);
        } else {
            firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, true, false);
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
