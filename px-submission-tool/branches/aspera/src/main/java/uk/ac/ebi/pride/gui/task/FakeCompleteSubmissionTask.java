package uk.ac.ebi.pride.gui.task;

import uk.ac.ebi.pride.gui.data.SubmissionRecord;

/**
 * Fake a complete submission message
 * @author Rui Wang
 * @version $Id$
 */
public class FakeCompleteSubmissionTask extends AbstractWebServiceTask<String> {

    private SubmissionRecord submissionRecord;

    public FakeCompleteSubmissionTask(SubmissionRecord submissionRecord) {
        this.submissionRecord = submissionRecord;
    }

    @Override
    protected String doInBackground() throws Exception {
        return "1-20120810-170431";
    }
}
