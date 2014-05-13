package uk.ac.ebi.pride.gui.task;

import uk.ac.ebi.pride.archive.submission.model.submission.SubmissionReferenceDetail;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;

/**
 * Fake a complete submission message
 * @author Rui Wang
 * @version $Id$
 */
public class FakeCompleteSubmissionTask extends AbstractWebServiceTask<SubmissionReferenceDetail> {

    private SubmissionRecord submissionRecord;

    public FakeCompleteSubmissionTask(SubmissionRecord submissionRecord) {
        this.submissionRecord = submissionRecord;
    }

    @Override
    protected SubmissionReferenceDetail doInBackground() throws Exception {
        return new SubmissionReferenceDetail("1-20120810-170431");
    }
}
