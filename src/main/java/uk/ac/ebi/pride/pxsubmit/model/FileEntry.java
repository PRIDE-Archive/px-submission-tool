package uk.ac.ebi.pride.pxsubmit.model;

import javafx.beans.property.*;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.data.model.DataFile;

import java.io.File;

/**
 * Observable wrapper for DataFile with UI-bindable properties.
 * Tracks validation state, upload progress, and checksum for each file.
 *
 * Usage in TableView:
 * - column.setCellValueFactory(new PropertyValueFactory<>("fileName"))
 * - progress can be bound to ProgressBar
 */
public class FileEntry {

    public enum ValidationState {
        PENDING,    // Not yet validated
        VALIDATING, // Currently being validated
        SUCCESS,    // Validation passed
        WARNING,    // Validation passed with warnings
        ERROR       // Validation failed
    }

    public enum UploadState {
        PENDING,    // Not yet uploaded
        UPLOADING,  // Currently uploading
        COMPLETED,  // Upload complete
        FAILED      // Upload failed
    }

    // Underlying data file
    private final ObjectProperty<DataFile> dataFile = new SimpleObjectProperty<>();

    // File properties (derived from DataFile but observable)
    private final StringProperty fileName = new SimpleStringProperty();
    private final StringProperty filePath = new SimpleStringProperty();
    private final LongProperty fileSize = new SimpleLongProperty();
    private final ObjectProperty<ProjectFileType> fileType = new SimpleObjectProperty<>();

    // Validation
    private final ObjectProperty<ValidationState> validationState = new SimpleObjectProperty<>(ValidationState.PENDING);
    private final StringProperty validationMessage = new SimpleStringProperty();

    // Checksum
    private final StringProperty checksum = new SimpleStringProperty();
    private final BooleanProperty checksumCalculated = new SimpleBooleanProperty(false);

    // Upload progress
    private final ObjectProperty<UploadState> uploadState = new SimpleObjectProperty<>(UploadState.PENDING);
    private final LongProperty uploadedBytes = new SimpleLongProperty(0);
    private final DoubleProperty uploadProgress = new SimpleDoubleProperty(0);
    private final StringProperty uploadMessage = new SimpleStringProperty();

    public FileEntry() {
    }

    public FileEntry(DataFile dataFile) {
        setDataFile(dataFile);
    }

    public FileEntry(File file, ProjectFileType type) {
        DataFile df = new DataFile();
        df.setFile(file);
        df.setFileType(type);
        setDataFile(df);
    }

    /**
     * Set the underlying DataFile and update observable properties
     */
    public void setDataFile(DataFile file) {
        this.dataFile.set(file);
        if (file != null) {
            fileName.set(file.getFileName());
            filePath.set(file.getFilePath());
            fileSize.set(file.getFile() != null ? file.getFile().length() : 0);
            fileType.set(file.getFileType());
        }
    }

    /**
     * Get the underlying DataFile
     */
    public DataFile getDataFile() {
        return dataFile.get();
    }

    public ObjectProperty<DataFile> dataFileProperty() {
        return dataFile;
    }

    // ==================== File Properties ====================

    public String getFileName() { return fileName.get(); }
    public StringProperty fileNameProperty() { return fileName; }

    public String getFilePath() { return filePath.get(); }
    public StringProperty filePathProperty() { return filePath; }

    public long getFileSize() { return fileSize.get(); }
    public LongProperty fileSizeProperty() { return fileSize; }

    public ProjectFileType getFileType() { return fileType.get(); }
    public void setFileType(ProjectFileType type) {
        fileType.set(type);
        if (dataFile.get() != null) {
            dataFile.get().setFileType(type);
        }
    }
    public ObjectProperty<ProjectFileType> fileTypeProperty() { return fileType; }

    public File getFile() {
        return dataFile.get() != null ? dataFile.get().getFile() : null;
    }

    // ==================== Validation ====================

    public ValidationState getValidationState() { return validationState.get(); }
    public void setValidationState(ValidationState state) { validationState.set(state); }
    public ObjectProperty<ValidationState> validationStateProperty() { return validationState; }

    public String getValidationMessage() { return validationMessage.get(); }
    public void setValidationMessage(String message) { validationMessage.set(message); }
    public StringProperty validationMessageProperty() { return validationMessage; }

    public boolean isValid() {
        ValidationState state = validationState.get();
        return state == ValidationState.SUCCESS || state == ValidationState.WARNING;
    }

    // ==================== Checksum ====================

    public String getChecksum() { return checksum.get(); }
    public void setChecksum(String value) {
        checksum.set(value);
        checksumCalculated.set(value != null && !value.isEmpty());
    }
    public StringProperty checksumProperty() { return checksum; }

    public boolean isChecksumCalculated() { return checksumCalculated.get(); }
    public ReadOnlyBooleanProperty checksumCalculatedProperty() { return checksumCalculated; }

    // ==================== Upload ====================

    public UploadState getUploadState() { return uploadState.get(); }
    public void setUploadState(UploadState state) { uploadState.set(state); }
    public ObjectProperty<UploadState> uploadStateProperty() { return uploadState; }

    public long getUploadedBytes() { return uploadedBytes.get(); }
    public void setUploadedBytes(long bytes) {
        uploadedBytes.set(bytes);
        if (fileSize.get() > 0) {
            uploadProgress.set((double) bytes / fileSize.get());
        }
    }
    public LongProperty uploadedBytesProperty() { return uploadedBytes; }

    public double getUploadProgress() { return uploadProgress.get(); }
    public ReadOnlyDoubleProperty uploadProgressProperty() { return uploadProgress; }

    public String getUploadMessage() { return uploadMessage.get(); }
    public void setUploadMessage(String message) { uploadMessage.set(message); }
    public StringProperty uploadMessageProperty() { return uploadMessage; }

    public boolean isUploaded() {
        return uploadState.get() == UploadState.COMPLETED;
    }

    // ==================== Utility ====================

    /**
     * Format file size for display
     */
    public String getFormattedFileSize() {
        long size = fileSize.get();
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileEntry fileEntry = (FileEntry) o;
        DataFile thisFile = dataFile.get();
        DataFile otherFile = fileEntry.dataFile.get();
        if (thisFile == null || otherFile == null) return false;
        return thisFile.equals(otherFile);
    }

    @Override
    public int hashCode() {
        DataFile file = dataFile.get();
        return file != null ? file.hashCode() : 0;
    }

    @Override
    public String toString() {
        return fileName.get();
    }
}
