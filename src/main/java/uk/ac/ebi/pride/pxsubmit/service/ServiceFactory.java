package uk.ac.ebi.pride.pxsubmit.service;

import javafx.collections.ObservableList;
import uk.ac.ebi.pride.archive.dataprovider.utils.SubmissionTypeConstants;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;

import java.io.File;
import java.util.List;

/**
 * Lightweight singleton factory for creating services.
 * All methods are non-static instance methods so subclasses can override for testing.
 */
public class ServiceFactory {

    private static ServiceFactory instance = new ServiceFactory();

    public static ServiceFactory getInstance() { return instance; }

    public static void setInstance(ServiceFactory factory) { instance = factory; }

    public static void resetInstance() { instance = new ServiceFactory(); }

    public ApiService createApiService(String username, String password) {
        return new ApiService(username, password);
    }

    public AuthService createAuthService() {
        return new AuthService();
    }

    public ChecksumService createChecksumService(ObservableList<DataFile> files) {
        return new ChecksumService(files);
    }

    public ValidationService createValidationService(ObservableList<DataFile> files,
                                                     SubmissionTypeConstants type) {
        return new ValidationService(files, type);
    }

    public SdrfParserService createSdrfParserService(File sdrfFile) {
        return new SdrfParserService(sdrfFile);
    }

    public UploadManager createUploadManager(Submission submission, UploadDetail detail,
                                             UploadMethod method, boolean trainingMode) {
        return new UploadManager(submission, detail, method, trainingMode);
    }

    public FtpUploadService createFtpUploadService(List<DataFile> files, UploadDetail detail) {
        return new FtpUploadService(files, detail);
    }

    public AsperaUploadService createAsperaUploadService(List<DataFile> files,
                                                         UploadDetail detail, String ascpPath) {
        return new AsperaUploadService(files, detail, ascpPath);
    }
}
