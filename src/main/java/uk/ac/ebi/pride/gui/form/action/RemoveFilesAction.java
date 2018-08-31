package uk.ac.ebi.pride.gui.form.action;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.toolsuite.gui.blocker.DefaultGUIBlocker;
import uk.ac.ebi.pride.toolsuite.gui.blocker.GUIBlocker;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Action for selecting and adding files to be submitted
 *
 * @author Rui Wang
 * @version $Id$
 */
public class RemoveFilesAction extends AbstractAction {

    public RemoveFilesAction() {
        super(App.getInstance().getDesktopContext().getProperty("remove.file.button.title"),
                GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("remove.file.button.small.icon")));
        this.putValue(SHORT_DESCRIPTION, App.getInstance().getDesktopContext().getProperty("remove.file.button.tooltip"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      Submission submission = ((AppContext) App.getInstance().getDesktopContext()).getSubmissionRecord().getSubmission();
      submission.removeAllDataFiles();
//        // select file to submit
//        List<File> files = selectFiles();
//
//        // process the selected files or folder
//        FileRemovalTask task = new FileRemovalTask(files);
//
//        // set gui blocker
//        task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));
//
//        // execute file selection task
//        App.getInstance().getDesktopContext().addTask(task);
    }

//    /**
//     * Show a file chooser dialog for selecting file
//     * Note: here both files and folders are allowed
//     *
//     * @return List<File>  a list of files and folders been selected
//     */
//    private List<File> selectFiles() {
//        // get application context
//        AppContext context = (AppContext) App.getInstance().getDesktopContext();
//
//        // create file chooser
//        JFileChooser fileChooser = new JFileChooser(context.getOpenFilePath());
//
//        // set file chooser title
//        fileChooser.setDialogTitle(context.getProperty("add.file.dialog.message"));
//
//        // set dialog type
//        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
//
//        // set selection mode
//        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
//        fileChooser.setMultiSelectionEnabled(true);
//
//        // show dialog
//        int result = fileChooser.showDialog(((App) App.getInstance()).getMainFrame(), null);
//
//        List<File> filesToOpen = new ArrayList<File>();
//
//        // check the selection results from open fiel dialog
//        if (result == JFileChooser.APPROVE_OPTION) {
//            filesToOpen.addAll(Arrays.asList(fileChooser.getSelectedFiles()));
//            File selectedFile = fileChooser.getSelectedFile();
//            String filePath = selectedFile.getPath();
//            // remember the path has visited
//            context.setOpenFilePath(filePath.replace(selectedFile.getName(), ""));
//        }
//
//        return filesToOpen;
//    }

    /**
     * Task to process files or folders selected
     */
    private static class FileRemovalTask extends TaskAdapter<Void, DataFile> {

        private List<File> files;

        public FileRemovalTask(List<File> files) {
            this.files = files;
        }

        @Override
        protected Void doInBackground() throws Exception {
            for (File file : files) {
              removeDataFile(file);
            }
            return null;
        }

        /**
         * Remove data file object from a given file
         *
         * @param file given file
         */
        private void removeDataFile(File file) throws IOException {
            // by default it is other type
            MassSpecFileFormat format = MassSpecFileFormat.checkFormat(file);

            DataFile dataFile = format == null ? new DataFile(file, ProjectFileType.OTHER) : new DataFile(file, format);
                ((AppContext) App.getInstance().getDesktopContext()).removeDatafile(dataFile);
        }
    }
}
