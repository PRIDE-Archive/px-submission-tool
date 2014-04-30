package uk.ac.ebi.pride.gui.task;

import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.submission.SubmissionReferenceDetail;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.web.util.template.SecureRestTemplateFactory;

/**
 * Task to complete a submission after files have been uploaded
 *
 * @author Rui Wang
 * @version $Id$
 */
public class CompleteSubmissionTask extends TaskAdapter<SubmissionReferenceDetail, String> {

    private final SubmissionRecord submissionRecord;
    private final RestTemplate restTemplate;

    public CompleteSubmissionTask(SubmissionRecord submissionRecord) {
        this.submissionRecord = submissionRecord;
        this.restTemplate = SecureRestTemplateFactory.getTemplate(submissionRecord.getUserName(), submissionRecord.getPassword());
    }

    @Override
    protected SubmissionReferenceDetail doInBackground() throws Exception {

        DesktopContext context = App.getInstance().getDesktopContext();
        String baseUrl = context.getProperty("px.submission.complete.url");

        return restTemplate.postForObject(baseUrl, submissionRecord.getUploadDetail(), SubmissionReferenceDetail.class);
    }
}
