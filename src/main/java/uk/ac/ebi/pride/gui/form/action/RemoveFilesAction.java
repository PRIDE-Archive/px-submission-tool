package uk.ac.ebi.pride.gui.form.action;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.toolsuite.gui.blocker.DefaultGUIBlocker;
import uk.ac.ebi.pride.toolsuite.gui.blocker.GUIBlocker;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Action for removing all the files that were added for submission
 *
 * @author Suresh Hewapathirana
 */
public class RemoveFilesAction extends AbstractAction {

    public RemoveFilesAction() {
        super(App.getInstance().getDesktopContext().getProperty("remove.file.button.title"),
                GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("remove.file.button.small.icon")));
        this.putValue(SHORT_DESCRIPTION, App.getInstance().getDesktopContext().getProperty("remove.file.button.tooltip"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // process the selected files or folder
        FileRemovalTask task = new FileRemovalTask();

        // set gui blocker
        task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));

        // execute file selection task
        App.getInstance().getDesktopContext().addTask(task);
    }

    /**
     * Task to process files or folders selected
     */
    private static class FileRemovalTask extends TaskAdapter<Void, DataFile> {

        @Override
        protected Void doInBackground() throws Exception {
            List<DataFile> files = ((AppContext) App.getInstance().getDesktopContext()).getSubmissionRecord().getSubmission().getDataFiles();
            for (DataFile file: files){
                ((AppContext) App.getInstance().getDesktopContext()).removeDatafile(file);
            }
            return null;
        }
    }
}
