package uk.ac.ebi.pride.gui.task.ftp;

import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.toolsuite.gui.task.Task;

/**
 * Error message indicates upload has stopped with files failed to be uploaded
 *
 * @author Rui Wang
 * @version $Id$
 */
public class UploadStoppedMessage extends UploadMessage{

    private SubmissionRecord submissionRecord;

    public UploadStoppedMessage(Task source, SubmissionRecord submissionRecord) {
        super(source, null);
        this.submissionRecord = submissionRecord;
    }

    public SubmissionRecord getSubmissionRecord() {
        return submissionRecord;
    }
}
