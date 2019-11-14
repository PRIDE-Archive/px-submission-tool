package uk.ac.ebi.pride.gui.form.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.data.util.MassSpecFileFormat;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.toolsuite.gui.blocker.DefaultGUIBlocker;
import uk.ac.ebi.pride.toolsuite.gui.blocker.GUIBlocker;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.gui.form.comp.NonOpaquePanel;
import uk.ac.ebi.pride.gui.navigation.NavigationControlPanel;
import uk.ac.ebi.pride.toolsuite.gui.task.Task;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskEvent;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskListener;
import uk.ac.ebi.pride.gui.util.ColourUtil;
import uk.ac.ebi.pride.gui.util.HttpUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

/**
 * Action triggered when loading a submission file
 *
 * @author Rui Wang
 * @version $Id$
 */
public class LoadSubmissionFileAction extends AbstractAction {

    public static final Logger logger = LoggerFactory.getLogger(LoadSubmissionFileAction.class);

    public LoadSubmissionFileAction() {
        super(App.getInstance().getDesktopContext().getProperty("load.submission.file.button.title"),
                GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("load.submission.file.button.small.icon")));
        this.putValue(SHORT_DESCRIPTION, App.getInstance().getDesktopContext().getProperty("load.submission.file.button.tooltip"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        App app = (App) App.getInstance();
        JDialog warningDialog = new LoadSubmissionFileDialog(app.getMainFrame());
        warningDialog.setLocationRelativeTo(app.getMainFrame());
        warningDialog.setVisible(true);
    }


    private static class LoadSubmissionFileDialog extends JDialog implements ActionListener, TaskListener<Void, Void> {
        private static final String CANCEL_ACTION_COMMAND = "Cancel";
        private static final String Load_ACTION_COMMAND = "Load";

        private JButton loadButton;


        private LoadSubmissionFileDialog(Frame owner) {
            super(owner);
            initComponents();
        }

        /**
         * Create GUI components
         */
        private void initComponents() {
            this.setSize(new Dimension(500, 250));

            JPanel contentPanel = new JPanel(new BorderLayout());
            this.setContentPane(contentPanel);

            // create table panel
            initWarningPanel();

            // create button panel
            initControlPanel();

            this.setContentPane(contentPanel);
        }

        private void initWarningPanel() {
            JPanel warningPanel = new JPanel(new BorderLayout());
            warningPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // create table title label
            JLabel label = new JLabel(App.getInstance().getDesktopContext().getProperty("bulk.submission.warning.title"));
            label.setFont(label.getFont().deriveFont(Font.BOLD).deriveFont(16f));
            warningPanel.add(label, BorderLayout.NORTH);

            // create warning message
            JLabel message = new JLabel(App.getInstance().getDesktopContext().getProperty("bulk.submission.warning.message"));
            warningPanel.add(message, BorderLayout.CENTER);

            // create summary file format
            Icon icon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("bulk.submission.summary.file.format.small.icon"));
            JButton fileFormat = GUIUtilities.createLabelLikeButton(icon, App.getInstance().getDesktopContext().getProperty("bulk.submission.summary.file.format"));
            fileFormat.setForeground(ColourUtil.HYPERLINK_COLOUR);
            fileFormat.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    HttpUtil.openURL(App.getInstance().getDesktopContext().getProperty("px.submission.summary.file.format.url"));
                }
            });
            warningPanel.add(fileFormat, BorderLayout.SOUTH);

            this.getContentPane().add(warningPanel, BorderLayout.CENTER);
        }

        private void initControlPanel() {
            // setup main pane
            JPanel controlPanel = new NavigationControlPanel();
            controlPanel.setLayout(new BorderLayout());

            // app context
            DesktopContext appContext = App.getInstance().getDesktopContext();

            // control pane
            JPanel ctrlPane = new NonOpaquePanel(new FlowLayout(FlowLayout.RIGHT));

            // cancel button
            JButton cancelButton = new JButton(appContext.getProperty("cancel.button.label"),
                    GUIUtilities.loadIcon(appContext.getProperty("cancel.button.small.icon")));
            cancelButton.setPreferredSize(new Dimension(90, 30));
            cancelButton.setActionCommand(CANCEL_ACTION_COMMAND);
            cancelButton.addActionListener(this);
            ctrlPane.add(cancelButton);

            // next button
            loadButton = new JButton(appContext.getProperty("bulk.submission.load.summary.file.button.label"),
                    GUIUtilities.loadIcon(appContext.getProperty("bulk.submission.load.summary.file.button.small.icon")));
            loadButton.setPreferredSize(new Dimension(90, 30));
            loadButton.setActionCommand(Load_ACTION_COMMAND);
            loadButton.addActionListener(this);
            ctrlPane.add(loadButton);

            controlPanel.add(ctrlPane, BorderLayout.EAST);

            this.getContentPane().add(controlPanel, BorderLayout.SOUTH);
        }

        /**
         * Show a file chooser dialog for selecting file
         *
         * @return File  submission file been selected
         */
        private File selectFile() {
            // get application context
            AppContext context = (AppContext) App.getInstance().getDesktopContext();

            // create file chooser
            JFileChooser fileChooser = new JFileChooser(context.getOpenFilePath());

            // set file chooser title
            fileChooser.setDialogTitle(context.getProperty("add.summary.file.dialog.message"));

            // set dialog type
            fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);

            // set selection mode
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);

            // show dialog
            int result = fileChooser.showDialog(((App) App.getInstance()).getMainFrame(), null);

            File fileToOpen = null;

            // check the selection results from open fiel dialog
            if (result == JFileChooser.APPROVE_OPTION) {
                fileToOpen = fileChooser.getSelectedFile();
                String filePath = fileToOpen.getPath();
                // remember the path has visited
                context.setOpenFilePath(filePath.replace(fileToOpen.getName(), ""));
            }

            logger.debug("selectFile(): fileToOpen = " + fileToOpen.getAbsolutePath());
            return fileToOpen;
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            String evtName = e.getActionCommand();

            if (CANCEL_ACTION_COMMAND.equals(evtName)) {
                this.dispose();
            } else if (Load_ACTION_COMMAND.equals(evtName)) {
                File submissionFile = selectFile();

                if (submissionFile != null) {
                    Task task = new LoadSubmissionFileTask(submissionFile);
                    task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));
                    task.addTaskListener(this);
                    App.getInstance().getDesktopContext().addTask(task);
                }


            }
        }

        @Override
        public void started(TaskEvent<Void> event) {
            Icon icon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("bulk.submission.loading.summary.file.small.icon"));
            loadButton.setIcon(icon);
        }

        @Override
        public void finished(TaskEvent<Void> event) {
            Icon icon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("bulk.submission.load.summary.file.button.small.icon"));
            loadButton.setIcon(icon);
            this.dispose();
        }

        @Override
        public void process(TaskEvent<List<Void>> event) {
        }

        @Override
        public void failed(TaskEvent<Throwable> event) {
        }

        @Override
        public void succeed(TaskEvent<Void> event) {
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

    /**
     * Task to process files or folders selected
     */
    private static class LoadSubmissionFileTask extends TaskAdapter<Void, Void> {

        private File file;

        public LoadSubmissionFileTask(File file) {
            this.file = file;
        }

        @Override
        protected Void doInBackground() throws Exception {

            Submission submission = SubmissionFileParser.parse(file);

            List<DataFile> dataFiles = submission.getDataFiles();
            logger.debug("doInBackground(): dataFiles.size() = " + dataFiles.size());

            for (DataFile dataFile : dataFiles) {
                MassSpecFileFormat fileFormat = dataFile.getFileFormat();

                logger.debug("doInBackground(): fileFormat = " + fileFormat);
                dataFile.setFileFormat(fileFormat);
            }

            SubmissionRecord submissionRecord = new SubmissionRecord(submission);

            AppContext ctx = (AppContext) App.getInstance().getDesktopContext();
            submissionRecord.getSubmission().getProjectMetaData()
                    .setSubmitterContact(ctx.getSubmissionRecord().getSubmission().getProjectMetaData()
                            .getSubmitterContact());
            ctx.setSubmissionRecord(submissionRecord);
            ctx.setBulkMode(true);

            return null;
        }
    }
}

