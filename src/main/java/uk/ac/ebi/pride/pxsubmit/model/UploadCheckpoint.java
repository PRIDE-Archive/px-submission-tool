package uk.ac.ebi.pride.pxsubmit.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.*;

/**
 * Checkpoint data saved before upload begins, updated per-file during upload,
 * and deleted after successful submission. Enables crash-recovery and resume.
 *
 * Serialized to ~/.pxsubmit/upload-checkpoint.json via Jackson.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadCheckpoint {

    private String userName;
    private String submissionPxPath;
    private String uploadMethod;
    private String uploadHost;
    private int uploadPort;
    private String uploadFolder;
    private boolean resubmissionMode;
    private String resubmissionAccession;
    private List<FileEntry> files = new ArrayList<>();
    private Set<String> uploadedFileNames = new HashSet<>();
    private Map<String, String> checksums = new HashMap<>();
    private long timestamp;

    public UploadCheckpoint() {}

    // Getters and setters

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getSubmissionPxPath() { return submissionPxPath; }
    public void setSubmissionPxPath(String submissionPxPath) { this.submissionPxPath = submissionPxPath; }

    public String getUploadMethod() { return uploadMethod; }
    public void setUploadMethod(String uploadMethod) { this.uploadMethod = uploadMethod; }

    public String getUploadHost() { return uploadHost; }
    public void setUploadHost(String uploadHost) { this.uploadHost = uploadHost; }

    public int getUploadPort() { return uploadPort; }
    public void setUploadPort(int uploadPort) { this.uploadPort = uploadPort; }

    public String getUploadFolder() { return uploadFolder; }
    public void setUploadFolder(String uploadFolder) { this.uploadFolder = uploadFolder; }

    public boolean isResubmissionMode() { return resubmissionMode; }
    public void setResubmissionMode(boolean resubmissionMode) { this.resubmissionMode = resubmissionMode; }

    public String getResubmissionAccession() { return resubmissionAccession; }
    public void setResubmissionAccession(String resubmissionAccession) { this.resubmissionAccession = resubmissionAccession; }

    public List<FileEntry> getFiles() { return files; }
    public void setFiles(List<FileEntry> files) { this.files = files; }

    public Set<String> getUploadedFileNames() { return uploadedFileNames; }
    public void setUploadedFileNames(Set<String> uploadedFileNames) { this.uploadedFileNames = uploadedFileNames; }

    public Map<String, String> getChecksums() { return checksums; }
    public void setChecksums(Map<String, String> checksums) { this.checksums = checksums; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    /**
     * Represents a single file in the checkpoint.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileEntry {
        private String filePath;
        private String fileName;
        private String fileType;

        public FileEntry() {}

        public FileEntry(String filePath, String fileName, String fileType) {
            this.filePath = filePath;
            this.fileName = fileName;
            this.fileType = fileType;
        }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
    }
}
