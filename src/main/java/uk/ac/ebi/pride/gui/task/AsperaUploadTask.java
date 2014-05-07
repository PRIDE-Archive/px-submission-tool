package uk.ac.ebi.pride.gui.task;

import com.asperasoft.faspmanager.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.submission.DropBoxDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.io.SubmissionFileWriter;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.gui.aspera.AsperaFileUploader;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.gui.task.ftp.*;
import uk.ac.ebi.pride.gui.util.Constant;
import uk.ac.ebi.pride.gui.util.OSDetector;
import uk.ac.ebi.pride.gui.util.SubmissionRecordSerializer;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by ilias
 */

public class AsperaUploadTask extends TaskAdapter<Void, UploadMessage> implements TransferListener {

    public static final Logger logger = LoggerFactory.getLogger(AsperaUploadTask.class);

    private SubmissionRecord submissionRecord;
    /**
     * Map contains files need to be submitted along with the folder name
     */
    private Set<File> fileToSubmit;

    /**
     * Total file size need to be uploaded
     */
    private long totalFileSize;

    /**
     * Constructor used for a new submission
     *
     * @param submissionRecord submission record
     */
    public AsperaUploadTask(SubmissionRecord submissionRecord) {
        this.submissionRecord = submissionRecord;
        this.fileToSubmit = Collections.synchronizedSet(new LinkedHashSet<File>());
        this.totalFileSize = 0;
    }

    @Override
    protected Void doInBackground() throws Exception {
        // save submission initial progress
        serializeSubmissionReport();

        // prepare for ftp upload
        prepareSubmission();

        // upload via aspera
        asperaUpload();

        // wait for aspera to upload
        waitUpload();

        return null;
    }

    private void waitUpload() throws InitializationException {
        final FaspManager faspManager = FaspManager.getSingleton();
        // this is keep the fasp manager running
        while (faspManager.isRunning()) {
        }
    }

    private void asperaUpload() throws FaspManagerException {

        // choose aspera binary according to operating system
        String ascpLocation = chooseAsperaBinary();

        File executable = new File(ascpLocation);

        // set aspera connection details
        AsperaFileUploader uploader = new AsperaFileUploader(executable);
        final UploadDetail uploadDetail = submissionRecord.getUploadDetail();
        final DropBoxDetail dropBox = uploadDetail.getDropBox();
        uploader.setRemoteLocation(uploadDetail.getHost(), dropBox.getUserName(), dropBox.getPassword());
//        uploader.setRemoteLocation("ah01.ebi.ac.uk", "pride-drop-010", "2VJFuR2u");

        // set upload parameters
        XferParams params = AsperaFileUploader.defaultTransferParams();
        params.createPath = true;
        uploader.setTransferParameters(params);

        // add transfer litener
        uploader.setListener(this);

        // start upload
        String transferId = uploader.uploadFiles(fileToSubmit, uploadDetail.getFolder());
        logger.debug("TransferEvent ID: {}", transferId);
    }

    private String chooseAsperaBinary() {
        //detect Operating System
        final OSDetector.OS os = OSDetector.getOS();
        final DesktopContext appContext = App.getInstance().getDesktopContext();

        // get aspera client binary
        String ascpLocation = "";

        switch (os) {
            case MAC:
                ascpLocation = appContext.getProperty("aspera.client.mac.binary");
                break;
            case LINUX_32:
                ascpLocation = appContext.getProperty("aspera.client.linux32.binary");
                break;
            case LINUX_64:
                ascpLocation = appContext.getProperty("aspera.client.linux64.binary");
                break;
            case WINDOWS:
                ascpLocation = appContext.getProperty("aspera.client.windows.binary");
                break;
            default:
                String msg = "Unsupported platform detected:" + OSDetector.os + " arch: " + OSDetector.arch;
                logger.error(msg);
                publish(new UploadErrorMessage(this, null, msg));
        }

        return ascpLocation;
    }

    /**
     * Prepare for upload an entire submission
     */
    private void prepareSubmission() {
        logger.debug("Preparing for uploading an entire submission");

        // add submission summary file
        File submissionFile = createSubmissionFile(); //submission px file creation
        if (submissionFile != null) {
            fileToSubmit.add(submissionFile); //add the submission px file to the upload list
        }

        // prepare for submission
        for (DataFile dataFile : submissionRecord.getSubmission().getDataFiles()) {
            totalFileSize += dataFile.getFile().length();
            if (dataFile.isFile()) {
                fileToSubmit.add(dataFile.getFile());
            }
        }
    }

    /**
     * Create submission file
     *
     * @return boolean true indicates success
     */
    private File createSubmissionFile() {
        try {
            // create a random temporary directory
            SecureRandom random = new SecureRandom();
            File tempDir = new File(System.getProperty("java.io.tmpdir") + File.separator + random.nextLong());
            tempDir.mkdir();

            File submissionFile = new File(tempDir.getAbsolutePath() + File.separator + Constant.PX_SUBMISSION_SUMMARY_FILE);

            logger.debug("Create temporary submission summary file : " + submissionFile.getAbsolutePath());

            // write out submission details
            SubmissionFileWriter.write(submissionRecord.getSubmission(), submissionFile);

            return submissionFile;
        } catch (SubmissionFileException ex) {
            String msg = "Failed to create submission file";
            logger.error(msg, ex);
            publish(new UploadErrorMessage(this, null, msg));
        }
        return null;
    }

    private void serializeSubmissionReport() {
        try {
            SubmissionRecordSerializer.serialize(submissionRecord);
        } catch (IOException ioe) {
            logger.error("Failed to save submission record");
        }
    }

    @Override
    protected void cancelled() {
        publish(new UploadStoppedMessage(this, submissionRecord));
    }

    @Override
    public void fileSessionEvent(TransferEvent transferEvent, SessionStats sessionStats, FileInfo fileInfo) {
        int totalNumOfFiles = submissionRecord.getSubmission().getDataFiles().size();

        switch (transferEvent) {
            case PROGRESS:
                int uploadedNumOfFiles = (int) sessionStats.getFilesComplete();
                logger.debug("Aspera transfer in progress");
                logger.debug("Total files: ");
                logger.debug("Total files: " + totalNumOfFiles);
                logger.debug("Files uploaded: " + uploadedNumOfFiles);
                logger.debug("Total file size " + totalFileSize);
                logger.debug("Uploaded file size " + sessionStats.getTotalTransferredBytes());
                publish(new UploadProgressMessage(this, null, totalFileSize, sessionStats.getTotalTransferredBytes(), totalNumOfFiles, uploadedNumOfFiles));
                break;
            case SESSION_STOP:
                FaspManager.destroy();
                publish(new UploadProgressMessage(this, null, totalFileSize, totalFileSize, totalNumOfFiles, totalNumOfFiles));
                publish(new UploadSuccessMessage(this));
                logger.debug("Aspera Session Stop");
                break;
            case SESSION_ERROR:
                logger.debug("Aspera session Error: " + transferEvent.getDescription());
                FaspManager.destroy();
                publish(new UploadErrorMessage(this, null, "Failed to upload via Aspera: " + transferEvent.getDescription()));
                break;
        }
    }
}
