package uk.ac.ebi.pride.gui.task.checksum;

import uk.ac.ebi.pride.toolsuite.gui.task.Task;
import uk.ac.ebi.pride.utilities.util.Tuple;

public class ChecksumMessage {
    private Task source;
    private Tuple<String,String> fileChecksum;
    int noOfFilesProcessed;
    int totalNoOfFiles;

    public ChecksumMessage(Task source, Tuple<String, String> fileChecksum, int noOfFilesProcessed, int totalNoOfFiles) {
        this.source = source;
        this.fileChecksum = fileChecksum;
        this.noOfFilesProcessed = noOfFilesProcessed;
        this.totalNoOfFiles = totalNoOfFiles;
    }

    public Tuple<String, String> getFileChecksum() {
        return fileChecksum;
    }

    public void setFileChecksum(Tuple<String, String> fileChecksum) {
        this.fileChecksum = fileChecksum;
    }

    public int getNoOfFilesProcessed() {
        return noOfFilesProcessed;
    }

    public void setNoOfFilesProcessed(int noOfFilesProcessed) {
        this.noOfFilesProcessed = noOfFilesProcessed;
    }

    public int getTotalNoOfFiles() {
        return totalNoOfFiles;
    }

    public void setTotalNoOfFiles(int totalNoOfFiles) {
        this.totalNoOfFiles = totalNoOfFiles;
    }
}
