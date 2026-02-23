package uk.ac.ebi.pride.pxsubmit.service;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.pxsubmit.model.TransferStatistics;

import java.util.List;

/**
 * Base class for upload services (FTP and Aspera).
 * Extracts common properties, progress tracking, logging, and statistics.
 */
public abstract class AbstractUploadService extends Service<UploadResult> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final List<DataFile> files;
    protected final UploadDetail uploadDetail;

    // Progress properties (observable from UI)
    private final LongProperty totalBytes = new SimpleLongProperty(0);
    private final LongProperty uploadedBytes = new SimpleLongProperty(0);
    private final IntegerProperty totalFiles = new SimpleIntegerProperty(0);
    private final IntegerProperty uploadedFiles = new SimpleIntegerProperty(0);
    private final StringProperty currentFileName = new SimpleStringProperty("");

    // Status tracking
    private final ObservableList<String> uploadLog = FXCollections.observableArrayList();

    // Transfer statistics tracking
    protected volatile TransferStatistics transferStatistics;

    protected AbstractUploadService(List<DataFile> files, UploadDetail uploadDetail) {
        this.files = files;
        this.uploadDetail = uploadDetail;

        this.totalFiles.set(files.size());
        this.totalBytes.set(calculateTotalSize(files));
    }

    protected long calculateTotalSize(List<DataFile> files) {
        return files.stream()
                .filter(f -> f.getFile() != null)
                .mapToLong(f -> f.getFile().length())
                .sum();
    }

    // Property accessors for UI binding
    public LongProperty totalBytesProperty() { return totalBytes; }
    public LongProperty uploadedBytesProperty() { return uploadedBytes; }
    public IntegerProperty totalFilesProperty() { return totalFiles; }
    public IntegerProperty uploadedFilesProperty() { return uploadedFiles; }
    public StringProperty currentFileNameProperty() { return currentFileName; }
    public ObservableList<String> getUploadLog() { return uploadLog; }
    public TransferStatistics getTransferStatistics() { return transferStatistics; }

    /**
     * Log a message with timestamp to both the observable log list and SLF4J.
     */
    protected void log(String message) {
        String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
        String logMessage = "[" + timestamp + "] " + message;
        Platform.runLater(() -> uploadLog.add(logMessage));
        logger.info(message);
    }
}
