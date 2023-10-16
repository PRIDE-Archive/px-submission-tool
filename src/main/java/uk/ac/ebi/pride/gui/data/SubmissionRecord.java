package uk.ac.ebi.pride.gui.data;

import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * SubmissionRecord is used to store the details and the progress of a submission
 *
 * @author Rui Wang
 * @version $Id$
 */
public class SubmissionRecord implements Serializable {
    private Submission submission;
    private String userName;
    private String password;
    private UploadDetail uploadDetail;
    private boolean summaryFileUploaded = false;
    private Set<DataFile> uploadedFiles;

    public SubmissionRecord() {
        this.submission = new Submission();
        this.uploadDetail = null;
        this.uploadedFiles = Collections.synchronizedSet(new HashSet<>());
    }

    public SubmissionRecord(Submission submission) {
        this(submission, null);
    }

    public SubmissionRecord(Submission submission, UploadDetail uploadDetail) {
        this.submission = submission;
        this.uploadDetail = uploadDetail;
        this.uploadedFiles = Collections.synchronizedSet(new HashSet<>());
    }

    public Submission getSubmission() {
        return submission;
    }

    public void setSubmission(Submission submission) {
        this.submission = submission;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UploadDetail getUploadDetail() {
        return uploadDetail;
    }

    public void setUploadDetail(UploadDetail uploadDetail) {
        this.uploadDetail = uploadDetail;
    }

    public Set<DataFile> getUploadedFiles() {
        return uploadedFiles;
    }

    public void setUploadedFiles(Set<DataFile> uploadedFiles) {
        this.uploadedFiles.clear();
        if (uploadedFiles != null) {
            this.uploadedFiles = uploadedFiles;
        }
    }

    public void addUploadedFiles(DataFile dataFile) {
        uploadedFiles.add(dataFile);
    }

    public boolean isUploaded(DataFile dataFile) {
        return uploadedFiles.contains(dataFile);
    }

    public boolean isSummaryFileUploaded() {
        return summaryFileUploaded;
    }

    public void setSummaryFileUploaded(boolean summaryFileUploaded) {
        this.summaryFileUploaded = summaryFileUploaded;
    }
}
