package uk.ac.ebi.pride.gui.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.submission.model.submission.DropBoxDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.gui.aspera.*;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.gui.task.ftp.*;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * Task to perform an Aspera upload via ascp CLI.
 */
public class AsperaUploadTask extends AsperaGeneralTask implements AscpTransferListener {

    public static final Logger logger = LoggerFactory.getLogger(AsperaUploadTask.class);

    public AsperaUploadTask(SubmissionRecord submissionRecord) {
        super(submissionRecord);
    }

    @Override
    void asperaUpload() throws AscpTransferException, UnsupportedEncodingException {
        String ascpLocation = chooseAsperaBinary();
        logger.debug("Aspera binary location {}", ascpLocation);
        File executable = new File(ascpLocation);
        AsperaFileUploader uploader = new AsperaFileUploader(executable);
        UploadDetail uploadDetail = submissionRecord.getUploadDetail();
        DropBoxDetail dropBox = uploadDetail.getDropBox();
        uploader.setRemoteLocation(uploadDetail.getHost(), dropBox.getUserName(), dropBox.getPassword());
        uploader.setListener(this);
        final String folder = uploadDetail.getFolder();
        File folderFile = new File(folder);
        String transferId = uploader.uploadFiles(filesToSubmit, folderFile.getName());
        logger.debug("Transfer session ID: {}", transferId);
    }

    @Override
    public void fileSessionEvent(AscpTransferEvent transferEvent, AscpSessionStats sessionStats, AscpFileInfo fileInfo) {
        int totalNumOfFiles =
                filesToSubmit != null
                        ? filesToSubmit.size()
                        : (submissionRecord.getSubmission().getDataFiles().size() + 1);
        switch (transferEvent) {
            case PROGRESS:
                int uploadedNumOfFiles = (int) sessionStats.getFilesComplete();
                logger.debug("Aspera transfer in progress - {}/{} files", uploadedNumOfFiles, totalNumOfFiles);
                publish(
                        new UploadProgressMessage(
                                this,
                                null,
                                totalNumOfFiles,
                                uploadedNumOfFiles,
                                totalNumOfFiles,
                                uploadedNumOfFiles));
                break;
            case SESSION_STOP:
                publish(
                        new UploadProgressMessage(
                                this,
                                null,
                                totalNumOfFiles,
                                totalNumOfFiles,
                                totalNumOfFiles,
                                totalNumOfFiles));
                if ((int) sessionStats.getFilesComplete() >= totalNumOfFiles) {
                    publish(new UploadSuccessMessage(this));
                }
                logger.debug("Aspera Session Stop");
                break;
            case SESSION_ERROR:
                logger.debug("Aspera session Error: {}", transferEvent.getDescription());
                publish(
                        new UploadErrorMessage(
                                this,
                                null,
                                "Failed to upload via Aspera: " + transferEvent.getDescription()));
                break;
            default:
                break;
        }
    }
}
