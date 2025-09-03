package uk.ac.ebi.pride.gui.form;

import com.asperasoft.faspmanager.FaspManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;

import uk.ac.ebi.pride.archive.dataprovider.utils.SubmissionTypeConstants;
import uk.ac.ebi.pride.archive.submission.model.submission.SubmissionReferenceDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
import uk.ac.ebi.pride.data.model.Contact;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareNavigationPanelDescriptor;
import uk.ac.ebi.pride.gui.task.*;
import uk.ac.ebi.pride.gui.task.ftp.*;
import uk.ac.ebi.pride.gui.util.Constant;
import uk.ac.ebi.pride.gui.util.SubmissionRecordSerializer;
import uk.ac.ebi.pride.toolsuite.gui.blocker.DefaultGUIBlocker;
import uk.ac.ebi.pride.toolsuite.gui.blocker.GUIBlocker;
import uk.ac.ebi.pride.toolsuite.gui.task.Task;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskEvent;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskListenerAdapter;

import javax.help.HelpBroker;
import javax.swing.*;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.List;

/**
 * Navigation descriptor for submission form
 *
 * @author Rui Wang
 * @version $Id$
 */
public class SubmissionDescriptor extends ContextAwareNavigationPanelDescriptor implements PropertyChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionDescriptor.class);
    private static final String FEEDBACK_SUBMITTED = "feedbackSubmitted";

    /**
     * Listen to get ftp detail task
     */
    private UploadDetailTaskListener uploadDetailTaskListener;

    /**
     * Listen to ftp upload task
     */
    private UploadTaskListener uploadTaskListener;

    /**
     * Listen to create ftp directory task
     */
    private CreateFTPDirectoryTaskListener createFTPDirectoryTaskListener;

    /**
     * Listen to complete submission task
     */
    private CompleteSubmissionTaskListener completeSubmissionTaskListener;

    // Feedback system removed - no longer needed

    private boolean isFinished = false;
    private boolean isSucceed = false;
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public SubmissionDescriptor(String id, String title, String desc) {
        super(id, title, desc, new SubmissionForm());

        this.uploadDetailTaskListener = new UploadDetailTaskListener();
        this.uploadTaskListener = new UploadTaskListener();
        this.completeSubmissionTaskListener = new CompleteSubmissionTaskListener();
        this.createFTPDirectoryTaskListener = new CreateFTPDirectoryTaskListener();


        // add property change listener
        SubmissionForm form = (SubmissionForm) SubmissionDescriptor.this.getNavigationPanel();
        form.addPropertyChangeListener(this);
    }

    @Override
    public void getHelp() {
        HelpBroker helpBroker = appContext.getMainHelpBroker();
        if (helpBroker != null) {
            helpBroker.setCurrentID("help.submission");
        }
    }

    @Override
    public void beforeDisplayingPanel() {
        logger.debug("Before displaying the submission panel");

        final String resubmissionPxAccession = appContext.getSubmissionRecord().getSubmission().getProjectMetaData().getResubmissionPxAccession();
        if (resubmissionPxAccession != null) {
            final String newTitle = "Step 5: " + appContext.getProperty("submission.nav.desc.title") + " (5/5)";
            super.setTitle(newTitle);
        }
        // re-enable cancel button
        SubmissionForm form = (SubmissionForm) SubmissionDescriptor.this.getNavigationPanel();
        form.enableCancelButton(true);

        UploadDetail uploadDetail = updateAndGetPreviousUploadDetail();

        // get ftp details if null
        if (uploadDetail == null) {
            getUploadDetail(appContext.getSubmissionRecord().getSubmission());
        } else {
            firePropertyChange(BEFORE_DISPLAY_PANEL_PROPERTY, false, true);
        }

        // set the default upload message
        form.setUploadMessage(appContext.getProperty("upload.default.message"));
        form.setProgressMessage(appContext.getProperty("progress.default.message"));
    }

    public UploadDetail updateAndGetPreviousUploadDetail() {
        UploadDetail uploadDetail = appContext.getSubmissionRecord().getUploadDetail();
        UploadMethod userSelectedUploadMethod = appContext.getUploadMethod();
        if (uploadDetail != null && !userSelectedUploadMethod.equals(uploadDetail.getMethod())) {
            uploadDetail.setMethod(userSelectedUploadMethod);
            if (userSelectedUploadMethod.equals(UploadMethod.FTP)) {
                uploadDetail.setHost("ftp-private.ebi.ac.uk");
                uploadDetail.setPort(21);
            } else {
                uploadDetail.setHost("hx-fasp-1.ebi.ac.uk");
                uploadDetail.setPort(3301);
            }
        }
        return uploadDetail;
    }

    private void getUploadDetail(Submission submission) {
        // retrieve the upload protocol
        final String uploadProtocol = appContext.getUploadMethod().toString();
        logger.debug("Configured upload protocol: {}", uploadProtocol);
        // choose upload method
        boolean hasURLBasedDataFiles = hasURLBasedDataFiles(submission);
        UploadMethod method;
        if (hasURLBasedDataFiles || uploadProtocol.equalsIgnoreCase(Constant.FTP)) {
            method = UploadMethod.FTP;
            if (appContext.getSubmissionRecord().getUploadDetail() != null) {
                appContext.getSubmissionRecord().setUploadDetail(updateAndGetPreviousUploadDetail());
            }
        } else {
            // default is ASPERA
            method = UploadMethod.ASPERA;
        }
        logger.debug("Chosen upload protocol: {}", method);

        Contact contact = submission.getProjectMetaData().getSubmitterContact();
        Task task = new GetUploadDetailTask(method, contact.getUserName(), contact.getPassword());
        task.addTaskListener(uploadDetailTaskListener);
        task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));
        appContext.addTask(task);
    }

    @Override
    public void displayingPanel() {
        logger.debug("Displaying the submission panel");
        // upload files
        SubmissionRecord submissionRecord = appContext.getSubmissionRecord();
        final UploadDetail uploadDetail = submissionRecord.getUploadDetail();
        final UploadMethod uploadMethod = uploadDetail.getMethod();

        Task task = null;

        if (uploadMethod.equals(UploadMethod.FTP)) {
            // Get FTP directory creator from factory
            task = UploadServiceFactory.createFtpDirectoryTask(uploadDetail);
            task.addTaskListener(createFTPDirectoryTaskListener);
        } else if (uploadMethod.equals(UploadMethod.ASPERA)) {
            // start aspera upload straight away
            // Get FTP directory creator from factory
            task = UploadServiceFactory.createPersistedAsperaUploadTask(submissionRecord);
            task.addTaskListener(uploadTaskListener);
        }
        if (task != null) {
            task.addOwner(SubmissionDescriptor.this);
            task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));
            appContext.addTask(task);
        }
    }

    private boolean hasURLBasedDataFiles(Submission submission) {
        List<DataFile> dataFiles = submission.getDataFiles();
        for (DataFile dataFile : dataFiles) {
            if (dataFile.isUrl()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void beforeHidingForPreviousPanel() {
        logger.debug("Before hiding for previous panel");

        // show a option dialog to warning user that the download will be stopped
        logger.debug("SubmissionDescriptor::beforeHidingForPreviousPanel() - call");
        if (isFinished) {
            //clearSubmissionRecord();
            // We don't check for isSucceed because isFinished is only set when the upload finished with success
            logger.debug("SubmissionDescriptor::beforeHidingForPreviousPanel() - call _ isfinished");
            app.restart();
        } else {
            logger.debug("SubmissionDescriptor::beforeHidingForPreviousPanel() - call _ cancelUpload()");
            cancelUpload();
        }
    }

    private void clearSubmissionRecord() {
        SubmissionRecord submissionRecord = appContext.getSubmissionRecord();
        submissionRecord.setUploadDetail(null);
        submissionRecord.setSummaryFileUploaded(false);
        submissionRecord.setUploadedFiles(null);
    }
    
    /**
     * Clean up submission data and remove .px-tool folder
     */
    private void cleanupSubmissionData() {
        try {
            // Remove submission record
            SubmissionRecordSerializer.remove();
            
            // Reset application context
            appContext.resetDataFileEntryCount();
            appContext.setResubmission(false);
            
            // Create new submission record
            SubmissionRecord newSubmissionRecord = new SubmissionRecord();
            newSubmissionRecord.getSubmission().getProjectMetaData().setSubmissionType(SubmissionTypeConstants.COMPLETE);
            appContext.setSubmissionRecord(newSubmissionRecord);
            
            // Remove .px-tool folder
            File pxToolDir = new File(System.getProperty("user.home") + File.separator + Constant.PX_TOOL_USER_DIRECTORY);
            if (pxToolDir.exists()) {
                deleteDirectory(pxToolDir);
                logger.info("Removed .px-tool directory: {}", pxToolDir.getAbsolutePath());
            }
            
        } catch (Exception e) {
            logger.error("Error during cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Recursively delete a directory
     */
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }

    private void cancelUpload() {
        int n = JOptionPane.showConfirmDialog(((App) App.getInstance()).getMainFrame(),
                appContext.getProperty("stop.upload.dialog.message"),
                appContext.getProperty("stop.upload.dialog.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (n == 0) {
            appContext.cancelTasksByOwner(this);
            clearSubmissionRecord();
            firePropertyChange(BEFORE_HIDING_FOR_PREVIOUS_PANEL_PROPERTY, false, true);
        }
    }

    @Override
    public void beforeHidingForNextPanel() {
        logger.debug("SubmissionDescriptor::beforeHidingForNextPanel() - call");
        app.shutdown(null);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String evtName = evt.getPropertyName();
        if (evtName.equals(SubmissionForm.STOP_SUBMISSION_PROP)) {
            logger.debug("Cancel ongoing submission task");
            // stop current submission
            appContext.cancelTasksByOwner(this);
        } else if (evtName.equals(SubmissionForm.START_SUBMISSION_PROP)) {
            SubmissionRecord submissionRecord = appContext.getSubmissionRecord();
            logger.debug("Restart submission task: {} files", submissionRecord.getSubmission().getDataFiles().size() - submissionRecord.getUploadedFiles().size());

            // start again the current submission
            Task task = null;
            final UploadMethod method = submissionRecord.getUploadDetail().getMethod();
            if (method.equals(UploadMethod.FTP)) {
                task = new FTPUploadTask(submissionRecord);
            } else if (method.equals(UploadMethod.ASPERA)) {
                task = new AsperaUploadTask(appContext.getSubmissionRecord());
            }

            if (task != null) {
                task.addTaskListener(uploadTaskListener);
                task.addOwner(this);
                task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));
                appContext.addTask(task);
            }

            // enable cancel button
            SubmissionForm form = (SubmissionForm) SubmissionDescriptor.this.getNavigationPanel();
            form.enableCancelButton(true);

            // set uploading message
            form.setUploadMessage(appContext.getProperty("upload.default.message"));
        }
        // Feedback system removed - no longer needed
    }

    /**
     * Task listener for getting ftp details
     */
    private class UploadDetailTaskListener extends TaskListenerAdapter<UploadDetail, String> {

        @Override
        public void succeed(TaskEvent<UploadDetail> mapTaskEvent) {
            // store ftp details
            UploadDetail uploadDetail = mapTaskEvent.getValue();

            if (uploadDetail != null) {
                appContext.getSubmissionRecord().setUploadDetail(uploadDetail);
                firePropertyChange(BEFORE_DISPLAY_PANEL_PROPERTY, false, true);
            } else {
                logger.error("Cannot connect to Proteomexchange web service for upload credentials");
                // show error message dialog
                JOptionPane.showConfirmDialog(app.getMainFrame(),
                        appContext.getProperty("upload.detail.error.message"),
                        appContext.getProperty("upload.detail.error.title"),
                        JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Task listener for uploading submission files
     */
    private class UploadTaskListener extends TaskListenerAdapter<Void, UploadMessage> {

        @Override
        public void process(TaskEvent<List<UploadMessage>> listTaskEvent) {
            for (UploadMessage uploadMessage : listTaskEvent.getValue()) {
                if (uploadMessage instanceof UploadErrorMessage) {
                    // notify upload has encounter an error
                    handleErrorMessage((UploadErrorMessage) uploadMessage);
                } else if (uploadMessage instanceof UploadInfoMessage) {
                    // notify upload started for a particular file
                    handleInfoMessage((UploadInfoMessage) uploadMessage);
                } else if (uploadMessage instanceof UploadProgressMessage) {
                    // notify the progress of the upload
                    handleProgressMessage((UploadProgressMessage) uploadMessage);
                } else if (uploadMessage instanceof UploadStoppedMessage) {
                    // notify the upload has finished with error
                    handleStopMessage((UploadStoppedMessage) uploadMessage);
                } else if (uploadMessage instanceof UploadSuccessMessage) {
                    // upload has finished successfully
                    handleSuccessMessage((UploadSuccessMessage) uploadMessage);
                }
            }
        }

        /**
         * Handle error message
         *
         * @param message error message
         */
        private void handleErrorMessage(UploadErrorMessage message) {
            logger.debug("Handle error message: {}", message.getMessage());
            SubmissionForm form = (SubmissionForm) SubmissionDescriptor.this.getNavigationPanel();
            final String type = (appContext.getUploadMethod() != null) ? appContext.getUploadMethod().toString() : Constant.ASPERA;
            if (type.equals(Constant.ASPERA)) {
                // Use the detailed error message from the upload task instead of the generic property
                String errorMessage = message.getMessage();
                if (errorMessage == null || errorMessage.trim().isEmpty()) {
                    errorMessage = appContext.getProperty("upload.aspera.error.message");
                }
                
                // Create a scrollable HTML editor pane for better line break handling
                JEditorPane editorPane = new JEditorPane();
                editorPane.setContentType("text/html");
                editorPane.setEditable(false);
                
                // Convert line breaks to HTML breaks
                String htmlMessage = "<html><body style='font-family: monospace; font-size: 12px;'>" +
                    errorMessage.replace("\n", "<br>") + "</body></html>";
                editorPane.setText(htmlMessage);
                
                // Calculate proper size based on content
                int lineCount = errorMessage.split("\n").length;
                
                JScrollPane scrollPane = new JScrollPane(editorPane);
                scrollPane.setPreferredSize(new Dimension(600, Math.max(200, lineCount * 20 + 50)));
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                
                JOptionPane.showMessageDialog(app.getMainFrame(),
                        scrollPane,
                        "Upload Error",
                        JOptionPane.ERROR_MESSAGE);
                
                form.setUploadMessage("Aspera upload failed. Retry with FTP...");
                appContext.setUploadMethod(UploadMethod.FTP);
                getUploadDetail(appContext.getSubmissionRecord().getSubmission());
            } else {
                form.setUploadMessage(message.getMessage());
            }
        }

        /**
         * Handle info message
         *
         * @param message info message
         */
        private void handleInfoMessage(UploadInfoMessage message) {
            logger.debug("Handle info message: {}", message.getInfo());
            SubmissionForm form = (SubmissionForm) SubmissionDescriptor.this.getNavigationPanel();
            form.setUploadMessage(message.getInfo());
        }

        /**
         * Handle progress message
         *
         * @param message progress message
         */
        private void handleProgressMessage(UploadProgressMessage message) {
            SubmissionForm form = (SubmissionForm) SubmissionDescriptor.this.getNavigationPanel();
            form.setProgress(message.getByteToTransfer(), message.getBytesTransferred(), message.getTotalNumOfFiles(), message.getUploadNumOfFiles());
        }

        /**
         * Handle stop message
         *
         * @param message stop message
         */
        private void handleStopMessage(UploadStoppedMessage message) {
            SubmissionRecord submissionRecord = appContext.getSubmissionRecord();
            logger.debug("Handle stop message: {} files", submissionRecord.getSubmission().getDataFiles().size() - submissionRecord.getUploadedFiles().size());

            // stop: exiting aspera upload
            FaspManager.destroy();

            SubmissionForm form = (SubmissionForm) SubmissionDescriptor.this.getNavigationPanel();
            form.setUploadMessage(appContext.getProperty("upload.stop.message"));
            form.enableStartButton(true);
        }

        /**
         * Handle success message
         *
         * @param message success message
         */
        private void handleSuccessMessage(UploadSuccessMessage message) {
            logger.debug("Handle success message");
            SubmissionForm form = (SubmissionForm) SubmissionDescriptor.this.getNavigationPanel();
            form.setUploadMessage(appContext.getProperty("upload.success.message"));
            form.enabledSuccessButton(true);

            // complete submission task
            // Get submission task from factory
            Task task = UploadServiceFactory.createCompleteSubmissionTask(appContext.getSubmissionRecord());
            task.addTaskListener(completeSubmissionTaskListener);
            task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));
            appContext.addTask(task);
        }
    }

    // Feedback system removed - no longer needed

    /**
     * Complete submission task listener
     */
    private class CompleteSubmissionTaskListener extends TaskListenerAdapter<SubmissionReferenceDetail, String> {

        @Override
        public void succeed(TaskEvent<SubmissionReferenceDetail> stringTaskEvent) {
            SubmissionForm form = (SubmissionForm) SubmissionDescriptor.this.getNavigationPanel();
            form.showCompletionMessage(stringTaskEvent.getValue().getReference());
            
            // Clean up submission data and restart application
            cleanupSubmissionData();
            
            isFinished = true;
            isSucceed = true;

            // Enable finish button
            firePropertyChange(BEFORE_FINISH_PROPERTY, false, true);
        }

        @Override
        public void failed(TaskEvent<Throwable> event) {
            //todo: implement
        }
    }

    private class CreateFTPDirectoryTaskListener extends TaskListenerAdapter<Boolean, UploadMessage> {
        @Override
        public void process(TaskEvent<List<UploadMessage>> listTaskEvent) {
            for (UploadMessage uploadMessage : listTaskEvent.getValue()) {
                if (uploadMessage instanceof UploadErrorMessage) {
                    // notify upload has encounter an error
                    handleErrorMessage((UploadErrorMessage) uploadMessage);
                } else if (uploadMessage instanceof UploadSuccessMessage) {
                    // upload has finished successfully
                    handleSuccessMessage((UploadSuccessMessage) uploadMessage);
                }
            }
        }

        private void handleErrorMessage(UploadErrorMessage uploadMessage) {
            logger.debug("Handle error message: {}", uploadMessage.getMessage());
            SubmissionForm form = (SubmissionForm) SubmissionDescriptor.this.getNavigationPanel();
            form.setUploadMessage(uploadMessage.getMessage());

            // Use the detailed error message from the upload task
            String errorMessage = uploadMessage.getMessage();
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                errorMessage = appContext.getProperty("upload.error.message");
            }
            
            // Create a scrollable HTML editor pane for better line break handling
            JEditorPane editorPane = new JEditorPane();
            editorPane.setContentType("text/html");
            editorPane.setEditable(false);
            
            // Convert line breaks to HTML breaks
            String htmlMessage = "<html><body style='font-family: monospace; font-size: 12px;'>" +
                errorMessage.replace("\n", "<br>") + "</body></html>";
            editorPane.setText(htmlMessage);
            
            // Calculate proper size based on content
            int lineCount = errorMessage.split("\n").length;
            
            JScrollPane scrollPane = new JScrollPane(editorPane);
            scrollPane.setPreferredSize(new Dimension(600, Math.max(200, lineCount * 20 + 50)));
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            // show error message dialog
            JOptionPane.showMessageDialog(app.getMainFrame(),
                    scrollPane,
                    appContext.getProperty("upload.error.title"),
                    JOptionPane.ERROR_MESSAGE);
        }

        private void handleSuccessMessage(UploadSuccessMessage uploadMessage) {
            Task task = new FTPUploadTask(appContext.getSubmissionRecord());
            task.addTaskListener(uploadTaskListener);
            task.addOwner(SubmissionDescriptor.this);
            task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));
            appContext.addTask(task);
        }
    }
}
