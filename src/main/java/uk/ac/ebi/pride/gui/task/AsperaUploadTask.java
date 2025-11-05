package uk.ac.ebi.pride.gui.task;

import com.asperasoft.faspmanager.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.submission.model.submission.DropBoxDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.gui.aspera.AsperaFileUploader;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.gui.task.ftp.*;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * Task to perform an Aspera upload.
 */

public class AsperaUploadTask extends AsperaGeneralTask implements TransferListener {

    public static final Logger logger = LoggerFactory.getLogger(AsperaUploadTask.class);

    /**
     * Constructor used for a new submission
     * @param submissionRecord submission record
     */
    public AsperaUploadTask(SubmissionRecord submissionRecord) {
        super(submissionRecord);
    }

    /**
     * Handles uploading of files using Asopera
     * @throws FaspManagerException problems using the Aspera API to perform the upload
     * @throws UnsupportedEncodingException problems creating a temporary file
     */
    @Override
    void asperaUpload() throws FaspManagerException, UnsupportedEncodingException {
        String ascpLocation = chooseAsperaBinary();
        logger.debug("Aspera binary location {}", ascpLocation);
        File executable = new File(ascpLocation);
        AsperaFileUploader uploader = new AsperaFileUploader(executable);
        UploadDetail uploadDetail = submissionRecord.getUploadDetail();
        DropBoxDetail dropBox = uploadDetail.getDropBox();
        uploader.setRemoteLocation(uploadDetail.getHost(), dropBox.getUserName(), dropBox.getPassword());
        XferParams params = AsperaFileUploader.defaultTransferParams();
        params.createPath = true;
        uploader.setTransferParameters(params);
        uploader.setListener(this);
        final String folder = uploadDetail.getFolder();
        File folderFile = new File(folder);
        String transferId = uploader.uploadFiles(filesToSubmit, folderFile.getName());
        logger.debug("TransferEvent ID: {}", transferId);
    }

    /**
     * Processes a file session event.
     * @param transferEvent the transfer event
     * @param sessionStats the session status
     * @param fileInfo the file information
     */
    @Override
    public void fileSessionEvent(TransferEvent transferEvent, SessionStats sessionStats, FileInfo fileInfo) {
        // Calculate total files - use filesToSubmit.size() which includes submission file
        int totalNumOfFiles = filesToSubmit != null ? filesToSubmit.size() : 
                             (submissionRecord.getSubmission().getDataFiles().size() + 1);
        switch (transferEvent) {
            case PROGRESS:
                int uploadedNumOfFiles = (int) sessionStats.getFilesComplete();
                logger.debug("Aspera transfer in progress");
                logger.debug("Total files: {}", totalNumOfFiles);
                logger.debug("Files uploaded: {}", uploadedNumOfFiles);
                // Publish progress based on file count only (no MB/bytes calculation)
                publish(new UploadProgressMessage(this, null, totalNumOfFiles, uploadedNumOfFiles, totalNumOfFiles, uploadedNumOfFiles));
                break;
            case SESSION_STOP:
                // Publish final progress based on file count only
                publish(new UploadProgressMessage(this, null, totalNumOfFiles, totalNumOfFiles, totalNumOfFiles, totalNumOfFiles));
                if((int)sessionStats.getFilesComplete()==totalNumOfFiles+1) {
                publish(new UploadSuccessMessage(this));
                }
                logger.debug("Aspera Session Stop");
                break;
            case SESSION_ERROR:
                logger.debug("Aspera session Error: " + transferEvent.getDescription());
                publish(new UploadErrorMessage(this, null, "Failed to upload via Aspera: " + transferEvent.getDescription()));
                break;
        }
    }
}
