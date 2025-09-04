package uk.ac.ebi.pride.gui.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;

import uk.ac.ebi.pride.archive.dataprovider.utils.SubmissionTypeConstants;
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
    private static final Logger logger = LoggerFactory.getLogger(CompleteSubmissionTask.class);
    
    private final SubmissionRecord submissionRecord;
    private final RestTemplate restTemplate;

    public CompleteSubmissionTask(SubmissionRecord submissionRecord) {
        this.submissionRecord = submissionRecord;
        this.restTemplate = SecureRestTemplateFactory.getTemplate(submissionRecord.getUserName(), submissionRecord.getPassword());
    }

    @Override
    protected SubmissionReferenceDetail doInBackground() throws Exception {
        logger.info("Starting submission completion process for user: {}", submissionRecord.getUserName());
        
        DesktopContext context = App.getInstance().getDesktopContext();
        UploadDetail uploadDetail = submissionRecord.getUploadDetail();
        String baseUrl = context.getProperty("px.submission.complete.url");
        
        if(((AppContext) context).isResubmission()){
            logger.info("Processing resubmission completion");
            baseUrl = context.getProperty("px.resubmission.complete.url");
        } else {
            logger.info("Processing new submission completion");
        }
        
        logger.debug("Base URL: {}, Is resubmission: {}", baseUrl, ((AppContext) context).isResubmission());

        SubmissionReferenceDetail result = null;
        try {
            Properties props = System.getProperties();
            String proxyHost = props.getProperty("http.proxyHost");
            String proxyPort = props.getProperty("http.proxyPort");

            if (proxyHost != null && proxyPort != null) {
                logger.debug("Configuring proxy: {}:{}", proxyHost, proxyPort);
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
                SimpleClientHttpRequestFactory requestFactory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
                requestFactory.setProxy(proxy);
            }

            String ticketId = context.getProperty(TICKET_ID);
            logger.debug("Ticket ID from context: {}", ticketId != null ? "present" : "null");

            if(ticketId!=null && !ticketId.equals("")){
                logger.info("Using existing ticket ID for submission completion");
                return new SubmissionReferenceDetail(ticketId);
            }
            
            if(((AppContext) context).getSubmissionType().equals(SubmissionTypeConstants.AFFINITY)){
                baseUrl = baseUrl + "?submissionType=AFFINITY";
                logger.debug("Affinity submission detected, modified URL: {}", baseUrl);
            }
            
            logger.info("Making API call to complete submission. URL: {}", baseUrl);
            logger.debug("Upload detail being sent: {}", uploadDetail);
            
            // Make the API call and handle the response
            try {
                result = restTemplate.postForObject(baseUrl, uploadDetail, SubmissionReferenceDetail.class);
                logger.debug("API call completed, result: {}", result);
                
            } catch (Exception parseException) {
                logger.error("Failed to parse API response as SubmissionReferenceDetail: {}", parseException.getMessage());
                // Try to get raw response for debugging
                try {
                    String rawResponse = restTemplate.postForObject(baseUrl, uploadDetail, String.class);
                    logger.error("Raw response that failed to parse: {}", rawResponse);
                } catch (Exception rawException) {
                    logger.error("Failed to get raw response: {}", rawException.getMessage());
                }
                throw parseException;
            }
            
            if (result == null) {
                logger.error("Submission completion API call succeeded but returned null response. URL: {}", baseUrl);
            } else {
                logger.info("Submission completion successful. Reference: {}", result.getReference());
            }
            
        } catch(Exception ex){
            logger.error("Exception during submission completion. URL: {}, Exception type: {}, Message: {}", 
                       baseUrl, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            // port blocked, dealt with later
        }
        
        ((AppContext) context).setResubmission(false);
        return result;
    }
}
