package uk.ac.ebi.pride.gui.form;

import com.asperasoft.faspmanager.FaspManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.archive.submission.model.submission.SubmissionReferenceDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
import uk.ac.ebi.pride.data.model.Contact;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.gui.blocker.DefaultGUIBlocker;
import uk.ac.ebi.pride.gui.blocker.GUIBlocker;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareNavigationPanelDescriptor;
import uk.ac.ebi.pride.gui.task.*;
import uk.ac.ebi.pride.gui.task.ftp.*;
import uk.ac.ebi.pride.gui.util.Constant;
import uk.ac.ebi.pride.gui.util.SubmissionRecordSerializer;

import javax.help.HelpBroker;
import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Navigation descriptor for submission form
 *
 * @author Rui Wang
 * @version $Id$
 */
public class SubmissionDescriptor extends ContextAwareNavigationPanelDescriptor implements PropertyChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionDescriptor.class);

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
     * Listen to completion of the submission task
     */
    private CompleteSubmissionTaskListener completeSubmissionTaskListener;

    /**
     * State indicates whether a submission has finished
     */
    private boolean isFinished;

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

    /**
     * Method to be performed to show help document
     * Override this method to add help functionality
     */
    @Override
    public void getHelp() {
        HelpBroker hb = appContext.getMainHelpBroker();
        hb.showID("help.submission", "javax.help.SecondaryWindow", "main");
    }

    @Override
    public void beforeDisplayingPanel() {
        logger.debug("Before displaying the submission panel");

        // re-enable cancel button
        SubmissionForm form = (SubmissionForm) SubmissionDescriptor.this.getNavigationPanel();
        form.enableCancelButton(true);

        // get ftp details if null
        if (appContext.getSubmissionRecord().getUploadDetail() == null) {
            Submission submission = appContext.getSubmissionRecord().getSubmission();

            // get ftp details from PRIDE
            getUploadDetail(submission);
        } else {
            firePropertyChange(BEFORE_DISPLAY_PANEL_PROPERTY, false, true);
        }

        // set the default upload message
        form.setUploadMessage(appContext.getProperty("upload.default.message"));
        form.setProgressMessage(appContext.getProperty("progress.default.message"));
    }

    private void getUploadDetail(Submission submission) {
        // retrieve the upload protocol
        final String uploadProtocol = System.getProperty("px.upload.protocol", Constant.ASPERA);
        logger.debug("Configured upload protocol: {}", uploadProtocol);

        // choose upload method
        boolean hasURLBasedDataFiles = hasURLBasedDataFiles(submission);
        UploadMethod method;
        if (hasURLBasedDataFiles || uploadProtocol.equalsIgnoreCase(Constant.FTP)) {
            method = UploadMethod.FTP;
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

        // upload files and folders
        // upload files
        SubmissionRecord submissionRecord = appContext.getSubmissionRecord();
        final UploadDetail uploadDetail = submissionRecord.getUploadDetail();
        final UploadMethod uploadMethod = uploadDetail.getMethod();

        Task task = null;

        if (uploadMethod.equals(UploadMethod.FTP)) {
            // create ftp directory before uploading
            task = new CreateFTPDirectoryTask(uploadDetail);
//            task = new FakeCreateFTPDirectoryTask(uploadDetail);
            task.addTaskListener(createFTPDirectoryTaskListener);
        } else if (uploadMethod.equals(UploadMethod.ASPERA)) {
            // start aspera upload straightaway
            task = new PersistedAsperaUploadTask(submissionRecord);
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
        if (isFinished) {
            //clearSubmissionRecord();
            app.restart();
        } else {
            cancelUpload();
        }
    }

    private void clearSubmissionRecord() {
        SubmissionRecord submissionRecord = appContext.getSubmissionRecord();
        submissionRecord.setUploadDetail(null);
        submissionRecord.setSummaryFileUploaded(false);
        submissionRecord.setUploadedFiles(null);
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
            form.setUploadMessage(message.getMessage());
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

            // stop exiting aspera upload
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
            Task task = new CompleteSubmissionTask(appContext.getSubmissionRecord());
//            Task task = new FakeCompleteSubmissionTask(appContext.getSubmissionRecord());
            task.addTaskListener(completeSubmissionTaskListener);
            task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));
            appContext.addTask(task);
        }
    }

    /**
     * Complete submission task listener
     */
    private class CompleteSubmissionTaskListener extends TaskListenerAdapter<SubmissionReferenceDetail, String> {

        @Override
        public void succeed(TaskEvent<SubmissionReferenceDetail> stringTaskEvent) {
            SubmissionForm form = (SubmissionForm) SubmissionDescriptor.this.getNavigationPanel();
            form.showCompletionMessage(stringTaskEvent.getValue().getReference());

            isFinished = true;

            // removing submission record
            SubmissionRecordSerializer.remove();
            // reset application context
            appContext.resetDataFileEntryCount();
            SubmissionRecord newSubmissionRecord = new SubmissionRecord();
            newSubmissionRecord.getSubmission().getProjectMetaData().setSubmissionType(SubmissionType.COMPLETE);
            appContext.setSubmissionRecord(newSubmissionRecord);

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

            // show error message dialog
            JOptionPane.showConfirmDialog(app.getMainFrame(),
                    appContext.getProperty("upload.error.message"),
                    appContext.getProperty("upload.error.title"),
                    JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
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
