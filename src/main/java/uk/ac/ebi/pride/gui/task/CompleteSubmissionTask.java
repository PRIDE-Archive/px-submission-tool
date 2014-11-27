package uk.ac.ebi.pride.gui.task;

import org.apache.http.HttpHost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.submission.SubmissionReferenceDetail;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.web.util.template.SecureRestTemplateFactory;

import java.util.Properties;

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
        SubmissionReferenceDetail result = null;
        try {
            Properties props = System.getProperties();
            String proxyHost = props.getProperty("http.proxyHost");
            String proxyPort = props.getProperty("http.proxyPort");

            if (proxyHost != null && proxyPort != null) {
                HttpComponentsClientHttpRequestFactory factory = ((HttpComponentsClientHttpRequestFactory) restTemplate.getRequestFactory());
                DefaultHttpClient defaultHttpClient = (DefaultHttpClient) factory.getHttpClient();
                HttpHost proxy = new HttpHost(proxyHost.trim(), Integer.parseInt(proxyPort));
                defaultHttpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            }
            result = restTemplate.postForObject(baseUrl, submissionRecord.getUploadDetail(), SubmissionReferenceDetail.class);
        } catch(Exception ex){
            // port blocked, dealt with later
        }
        return result;
    }
}
