package uk.ac.ebi.pride.gui.form.action;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.blocker.DefaultGUIBlocker;
import uk.ac.ebi.pride.gui.blocker.GUIBlocker;
import uk.ac.ebi.pride.gui.task.TaskAdapter;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;

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
public class AddFileSelectionAction extends AbstractAction {

    public AddFileSelectionAction() {
        super(App.getInstance().getDesktopContext().getProperty("add.file.button.title"),
                GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("add.file.button.small.icon")));
        this.putValue(SHORT_DESCRIPTION, App.getInstance().getDesktopContext().getProperty("add.file.button.tooltip"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // select file to submit
        List<File> files = selectFiles();

        // process the selected files or folder
        FileSelectionTask task = new FileSelectionTask(files);

        // set gui blocker
        task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));

        // execute file selection task
        App.getInstance().getDesktopContext().addTask(task);
    }

    /**
     * Show a file chooser dialog for selecting file
     * Note: here both files and folders are allowed
     *
     * @return List<File>  a list of files and folders been selected
     */
    private List<File> selectFiles() {
        // get application context
        AppContext context = (AppContext) App.getInstance().getDesktopContext();

        // create file chooser
        JFileChooser fileChooser = new JFileChooser(context.getOpenFilePath());

        // set file chooser title
        fileChooser.setDialogTitle(context.getProperty("add.file.dialog.message"));

        // set dialog type
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);

        // set selection mode
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);

        // show dialog
        int result = fileChooser.showDialog(((App) App.getInstance()).getMainFrame(), null);

        List<File> filesToOpen = new ArrayList<File>();

        // check the selection results from open fiel dialog
        if (result == JFileChooser.APPROVE_OPTION) {
            filesToOpen.addAll(Arrays.asList(fileChooser.getSelectedFiles()));
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getPath();
            // remember the path has visited
            context.setOpenFilePath(filePath.replace(selectedFile.getName(), ""));
        }

        return filesToOpen;
    }

    /**
     * Task to process files or folders selected
     */
    private static class FileSelectionTask extends TaskAdapter<Void, DataFile> {

        private List<File> files;

        public FileSelectionTask(List<File> files) {
            this.files = files;
        }

        @Override
        protected Void doInBackground() throws Exception {

            for (File file : files) {
                if (file.isFile() && file.canRead()) {
                    // file
                    createDataFile(file);
                } else if (file.isDirectory() && file.canRead()) {
                    // folder
                    handleFolder(file);
                }
            }

            return null;
        }

        /**
         * Handle folder selection
         *
         * @param folder selected folder
         */
        private void handleFolder(File folder) throws IOException {
            // todo: warning about sub folder
            // todo: warning about hidden file
            // todo: warning about too many files
            File[] files = folder.listFiles();
            for (File innerFile : files) {
                // ignore hidden file or folder
                if (!innerFile.isHidden()) {
                    if (innerFile.isFile() && innerFile.canRead()) {
                        createDataFile(innerFile);
                    } else if (innerFile.isDirectory()) {
                        if (MassSpecFileFormat.isMassSpecDataFolder(innerFile)) {
                            createDataFile(innerFile);
                        } else {
                            handleFolder(innerFile);
                        }
                    }
                }
            }
        }

        /**
         * Create a new data file object from a given file
         *
         * @param file given file
         */
        private void createDataFile(File file) throws IOException {
            // by default it is other type
            MassSpecFileFormat format = MassSpecFileFormat.checkFormat(file);

            DataFile newDataFile = format == null ? new DataFile(file, ProjectFileType.OTHER) : new DataFile(file, format);
            if (!hasSameFileName(newDataFile)) {
                ((AppContext) App.getInstance().getDesktopContext()).addDataFile(newDataFile);
            }
        }


        /**
         * Check whether a given data file list contains
         *
         * @param dataFile data file
         * @return true means file with the same exists
         */
        private boolean hasSameFileName(DataFile dataFile) {
            // TODO - I recommend using a different data structure here that keeps track of DataFiles and their names
            // TODO - using a set for the names, otherwise, when the list of files is too long, this method is very
            // TODO - inefficient
            Submission submission = ((AppContext) App.getInstance().getDesktopContext()).getSubmissionRecord().getSubmission();
            List<DataFile> dataFiles = submission.getDataFiles();
            for (DataFile file : dataFiles) {
                String fileName = file.getFileName();
                if (fileName.equals(dataFile.getFileName())) {
                    return true;
                }
            }
            return false;
        }
    }
}
