package uk.ac.ebi.pride.gui.task.ftp;

import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.toolsuite.gui.task.Task;

/**
 * Message contains upload progress for a file
 *
 * @author Rui Wang
 * @version $Id$
 */
public class UploadProgressMessage extends UploadMessage {
    private long byteToTransfer;
    private long bytesTransferred;
    private int totalNumOfFiles = -1;
    private int uploadNumOfFiles = -1;

    public UploadProgressMessage(Task source, DataFile dataFile, long byteToTransfer, long bytesTransferred) {
        this(source, dataFile, byteToTransfer, bytesTransferred, -1, -1);
    }

    public UploadProgressMessage(Task source,
                                 DataFile dataFile,
                                 long byteToTransfer,
                                 long bytesTransferred,
                                 int totalNumOfFiles,
                                 int uploadedNumOfFiles) {
        super(source, dataFile);
        this.byteToTransfer = byteToTransfer;
        this.bytesTransferred = bytesTransferred;
        this.totalNumOfFiles = totalNumOfFiles;
        this.uploadNumOfFiles = uploadedNumOfFiles;
    }

    public long getByteToTransfer() {
        return byteToTransfer;
    }

    public long getBytesTransferred() {
        return bytesTransferred;
    }

    public int getTotalNumOfFiles() {
        return totalNumOfFiles;
    }

    public int getUploadNumOfFiles() {
        return uploadNumOfFiles;
    }
}
