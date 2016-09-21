package uk.ac.ebi.pride.gui.task;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.task
 * Timestamp: 2016-08-01 13:09
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This Factory will control which upload service instance will be used by the submission process
 *
 * It allows us to do things like preventing real file submissions when working in 'training mode'
 */
public class UploadServiceFactory {
    private static final AppContext getAppContext() {
        return (AppContext) App.getInstance().getDesktopContext();
    }

    /**
     * Create the object that will handle FTP directory creation
     * @param uploadDetail upload details
     * @return fake FTP directory creation task when working in training mode, real FTP directory creation task otherwise
     */
    public static Task createFtpDirectoryTask(UploadDetail uploadDetail) {
        if (getAppContext().isTrainingModeFlag()) {
            return new FakeCreateFTPDirectoryTask(uploadDetail);
        }
        return new CreateFTPDirectoryTask(uploadDetail);
    }

    /**
     * Creates the object that will handle data transfer when using aspera option
     * @param submissionRecord SubmissionRecord
     * @return when working in 'training mode', it will override transfer method to FTP Fake upload process,
     * otherwise, it will create the PersistedAsperaUploadTask to actually submit the files
     */
    public static Task createPersistedAsperaUploadTask(SubmissionRecord submissionRecord) {
        if (getAppContext().isTrainingModeFlag()) {
            return new FakeCreateFTPDirectoryTask(submissionRecord.getUploadDetail());
        }
        return new PersistedAsperaUploadTask(submissionRecord);
    }

    /**
     * Creates the object that will handle the complete submission (e.g. get the submission reference)
     * @param submissionRecord SubmissionRecord
     * @return it returns a fake submission object when in 'training mode', or the real one otherwise
     */
    public static Task createCompleteSubmissionTask(SubmissionRecord submissionRecord) {
        if (getAppContext().isTrainingModeFlag()) {
            return new FakeCompleteSubmissionTask(submissionRecord);
        }
        return new CompleteSubmissionTask(submissionRecord);
    }

    /**
     * Creates the object that will handle file upload via FTP
     * @param dataFile data file to upload
     * @param uploadDetail upload details
     * @return a fake FTP file uploader when working in 'training mode', the real one otherwise
     */
    public static Task createFileFtpUploadTask(DataFile dataFile, UploadDetail uploadDetail) {
        if (getAppContext().isTrainingModeFlag()) {
            return new FakeFileFTPUploadTask(dataFile, uploadDetail);
        }
        return new FileFTPUploadTask(dataFile, uploadDetail);
    }
}
