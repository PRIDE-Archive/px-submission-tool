package uk.ac.ebi.pride.gui.task;

import com.asperasoft.faspmanager.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.io.SubmissionFileWriter;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.gui.aspera.AsperaFileUploader;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.gui.task.ftp.UploadErrorMessage;
import uk.ac.ebi.pride.gui.task.ftp.UploadMessage;
import uk.ac.ebi.pride.gui.task.ftp.UploadProgressMessage;
import uk.ac.ebi.pride.gui.task.ftp.UploadSuccessMessage;
import uk.ac.ebi.pride.gui.util.Constant;
import uk.ac.ebi.pride.gui.util.SubmissionRecordSerializer;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

/**
 * Created by ilias
 */

public class AsperaUploadTask extends TaskAdapter<Void, UploadMessage> implements TransferListener {

    public static final Logger logger = LoggerFactory.getLogger(FTPUploadTask.class);

    private SubmissionRecord submissionRecord;
    /**
     * Map contains files need to be submitted along with the folder name
     */
    private Set<DataFile> fileToSubmit;
    /**
     * Map contains files have failed submission
     */
    private Set<DataFile> fileFailToSubmit;

    /**
     * Total file size need to be uploaded
     */
    private long totalFileSize;
    /**
     * Total file size been uploaded
     */
    private long uploadFileSize;

    /**
     * Number of ongoing sub tasks
     */
    private int ongoingSubTasks;
    /**
     * indicates whether the upload has already finished
     */
    private boolean uploadFinished;

    /**
     * Constructor used for a new submission
     *
     * @param submissionRecord submission record
     */
    public AsperaUploadTask(SubmissionRecord submissionRecord) {
        this.submissionRecord = submissionRecord;
        this.fileToSubmit = Collections.synchronizedSet(new LinkedHashSet<DataFile>());
        this.fileFailToSubmit = Collections.synchronizedSet(new LinkedHashSet<DataFile>());
        this.totalFileSize = 0;
        this.uploadFileSize = 0;
        this.ongoingSubTasks = 0;
        this.uploadFinished = false;
    }

    @Override
    protected Void doInBackground() throws Exception {
        // save submission initial progress
        serializeSubmissionReport();

        // prepare for ftp upload
        prepareSubmission();

        String ascpLocation = "./src/main/lib/bin/linux-64/ascp";
        File executable = new File(ascpLocation);
        AsperaFileUploader uploader = new AsperaFileUploader(executable);
        uploader.setRemoteLocation("ah01.ebi.ac.uk", "pride-drop-010", "2VJFuR2u");

        XferParams params = AsperaFileUploader.defaultTransferParams();
        params.createPath = true;
        uploader.setTransferParameters(params);

        List<File> uploadFiles = new ArrayList<File>(1);

        for (DataFile dataFile : fileToSubmit) {
            uploadFiles.add(dataFile.getFile());
        }
        uploader.setListener(this);
        String transferId = uploader.uploadFile(uploadFiles, "aspera_test");
        System.out.println("TransferEvent ID: " + transferId);
//        publish(new UploadSuccessMessage(this)); // move it to listener part
//        PUT MY ASPERA CODE HERE
//        publish message

        return null;
    }

    /**
     * Prepare for upload an entire submission
     */
    private void prepareSubmission() {
        logger.debug("Preparing for uploading an entire submission");

        // add submission summary file
//        if (!submissionRecord.isSummaryFileUploaded())
//        {
        File submissionFile = createSubmissionFile(); //submission px file creation
        if (submissionFile != null) {
            DataFile dataFile = new DataFile();
            dataFile.setFile(submissionFile);
            fileToSubmit.add(dataFile); //add the submission px file to the upload list
        }
//        }

        // prepare for submission
        for (DataFile dataFile : submissionRecord.getSubmission().getDataFiles()) {
            totalFileSize += dataFile.getFile().length();
            if (dataFile.isFile()) {
                fileToSubmit.add(dataFile);
            }

//            if (dataFile.isFile() && submissionRecord.isUploaded(dataFile))
//            {
//                uploadFileSize += dataFile.getFile().length();
//            }
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


    //    @Override
    public void fileSessionEvent(TransferEvent transferEvent, SessionStats sessionStats, FileInfo fileInfo) {
//        UploadMessage uploadMessage = new UploadProgressMessage()
        int totalNumOfFiles = submissionRecord.getSubmission().getDataFiles().size();

        if (transferEvent == TransferEvent.PROGRESS) {
            System.out.println("Transfer in Progress");
            System.out.println("Total files: ");
//            uploadFileSize += ((UploadProgressMessage) uploadMessage).getBytesTransferred();
            int uploadedNumOfFiles = (int) sessionStats.getFilesComplete();
            System.out.println("Total files: " + totalNumOfFiles);
            System.out.println("Files uploaded: " + uploadedNumOfFiles);
            System.out.println("Total file size " + totalFileSize);
            System.out.println("Uploaded file size " + sessionStats.getTotalTransferredBytes());
            publish(new UploadProgressMessage(this, null, totalFileSize, sessionStats.getTotalTransferredBytes(), totalNumOfFiles, uploadedNumOfFiles));
        }

        if (transferEvent == TransferEvent.SESSION_STOP) {
            publish(new UploadProgressMessage(this, null, totalFileSize, totalFileSize, totalNumOfFiles, totalNumOfFiles));
            publish(new UploadSuccessMessage(this));
            System.out.println("Session Stop");
            FaspManager.destroy();
        }

        if (transferEvent == TransferEvent.SESSION_ERROR) {
            System.out.println("Session Error");
//            logger.debug("Failed to upload file: " + uploadMessage.getDataFile().getFile().getName());

//            publish(new UploadErrorMessage())
            FaspManager.destroy();
        }

//        if (transferEvent == TransferEvent.FILE_STOP)
//        {
//            System.out.println(fileInfo.getName() + " DONE");
//        }
    }
}
