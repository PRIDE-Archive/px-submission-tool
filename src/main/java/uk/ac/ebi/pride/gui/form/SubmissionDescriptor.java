package uk.ac.ebi.pride.gui.form;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.data.model.Contact;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.gui.blocker.DefaultGUIBlocker;
import uk.ac.ebi.pride.gui.blocker.GUIBlocker;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareNavigationPanelDescriptor;
import uk.ac.ebi.pride.gui.task.*;
import uk.ac.ebi.pride.gui.task.ftp.*;
import uk.ac.ebi.pride.gui.util.SubmissionRecordSerializer;
import uk.ac.ebi.pride.prider.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.prider.webservice.submission.model.FtpUploadDetail;
import uk.ac.ebi.pride.prider.webservice.submission.model.SubmissionReferenceDetail;

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
    private FTPDetailTaskListener ftpDetailTaskListener;

    /**
     * Listen to ftp upload task
     */
    private FTPUploadTaskListener ftpUploadTaskListener;

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

        this.ftpDetailTaskListener = new FTPDetailTaskListener();
        this.ftpUploadTaskListener = new FTPUploadTaskListener();
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
        if (appContext.getSubmissionRecord().getFtpDetail() == null) {
            Submission submission = appContext.getSubmissionRecord().getSubmission();

            // get ftp details from PRIDE
            Contact contact = submission.getProjectMetaData().getSubmitterContact();
            Task task = new GetFTPDetailTask(contact.getUserName(), contact.getPassword());
            task.addTaskListener(ftpDetailTaskListener);
            task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));
            appContext.addTask(task);
        } else {
            firePropertyChange(BEFORE_DISPLAY_PANEL_PROPERTY, false, true);
        }

        // set the default upload message
        form.setUploadMessage(appContext.getProperty("ftp.upload.default.message"));
        form.setProgressMessage(appContext.getProperty("ftp.progress.default.message"));
    }

    @Override
    public void displayingPanel() {
        logger.debug("Displaying the submission panel");

        // upload files and folders
        // upload files
        Task task = new CreateFTPDirectoryTask(appContext.getSubmissionRecord().getFtpDetail());
        task.addTaskListener(createFTPDirectoryTaskListener);
        task.addOwner(this);
        task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));
        appContext.addTask(task);
    }

    @Override
    public void beforeHidingForPreviousPanel() {
        logger.debug("Before hiding for previous panel");

        // show a option dialog to warning user that the download will be stopped
        if (isFinished) {
            app.restart();
        } else {
            int n = JOptionPane.showConfirmDialog(((App) App.getInstance()).getMainFrame(),
                    appContext.getProperty("stop.upload.dialog.message"),
                    appContext.getProperty("stop.upload.dialog.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (n == 0) {
                appContext.cancelTasksByOwner(this);
                SubmissionRecord submissionRecord = appContext.getSubmissionRecord();
                submissionRecord.setFtpDetail(null);
                submissionRecord.setSummaryFileUploaded(false);
                submissionRecord.setUploadedFiles(null);
                firePropertyChange(BEFORE_HIDING_FOR_PREVIOUS_PANEL_PROPERTY, false, true);
            }
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
            Task task = new FTPUploadTask(submissionRecord);
            task.addTaskListener(ftpUploadTaskListener);
            task.addOwner(this);
            task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));
            appContext.addTask(task);

            // enable cancel button
            SubmissionForm form = (SubmissionForm) SubmissionDescriptor.this.getNavigationPanel();
            form.enableCancelButton(true);

            // set uploading message
            form.setUploadMessage(appContext.getProperty("ftp.upload.default.message"));
        }
    }

    /**
     * Task listener for getting ftp details
     */
    private class FTPDetailTaskListener extends TaskListenerAdapter<FtpUploadDetail, String> {

        @Override
        public void succeed(TaskEvent<FtpUploadDetail> mapTaskEvent) {
            // store ftp details
            FtpUploadDetail ftpUploadDetails = mapTaskEvent.getValue();

            if (ftpUploadDetails != null) {
                appContext.getSubmissionRecord().setFtpDetail(ftpUploadDetails);

                firePropertyChange(BEFORE_DISPLAY_PANEL_PROPERTY, false, true);
            } else {
                // show error message dialog
                JOptionPane.showConfirmDialog(app.getMainFrame(),
                        appContext.getProperty("ftp.upload.detail.error.message"),
                        appContext.getProperty("ftp.upload.detail.error.title"),
                        JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Task listener for uploading submission files
     */
    private class FTPUploadTaskListener extends TaskListenerAdapter<Void, UploadMessage> {

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

            SubmissionForm form = (SubmissionForm) SubmissionDescriptor.this.getNavigationPanel();
            form.setUploadMessage(appContext.getProperty("ftp.upload.stop.message"));
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
            form.setUploadMessage(appContext.getProperty("ftp.upload.success.message"));
            form.enabledSuccessButton(true);

            // complete submission task
            Task task = new CompleteSubmissionTask(appContext.getSubmissionRecord());
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
                    appContext.getProperty("ftp.upload.error.message"),
                    appContext.getProperty("ftp.upload.error.title"),
                    JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
        }

        private void handleSuccessMessage(UploadSuccessMessage uploadMessage) {
            Task task = new FTPUploadTask(appContext.getSubmissionRecord());
            task.addTaskListener(ftpUploadTaskListener);
            task.addOwner(SubmissionDescriptor.this);
            task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));
            appContext.addTask(task);
        }
    }
}
