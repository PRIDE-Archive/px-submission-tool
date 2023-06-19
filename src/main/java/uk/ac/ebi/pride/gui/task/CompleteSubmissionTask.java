package uk.ac.ebi.pride.gui.task;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.archive.submission.model.submission.SubmissionReferenceDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;
import uk.ac.ebi.pride.web.util.template.SecureRestTemplateFactory;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Properties;

import static uk.ac.ebi.pride.gui.util.Constant.TICKET_ID;

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
        UploadDetail uploadDetail = submissionRecord.getUploadDetail();
        String baseUrl = context.getProperty("px.submission.complete.url");
        if(((AppContext) context).isResubmission()){
            //uploadDetail = ((AppContext) context).getResubmissionRecord().getUploadDetail();
            baseUrl = context.getProperty("px.resubmission.complete.url");
        }


        SubmissionReferenceDetail result = null;
        try {
            Properties props = System.getProperties();
            String proxyHost = props.getProperty("http.proxyHost");
            String proxyPort = props.getProperty("http.proxyPort");

            if (proxyHost != null && proxyPort != null) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
                SimpleClientHttpRequestFactory requestFactory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
                requestFactory.setProxy(proxy);
            }

            String ticketId = context.getProperty(TICKET_ID);

            if(ticketId!=null && !ticketId.equals("")){
                return new SubmissionReferenceDetail(ticketId);
            }
            result = restTemplate.postForObject(baseUrl, uploadDetail, SubmissionReferenceDetail.class);
        } catch(Exception ex){
            // port blocked, dealt with later
        }
        ((AppContext) context).setResubmission(false);
        return result;
    }
}
