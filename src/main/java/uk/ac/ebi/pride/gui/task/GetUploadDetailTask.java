package uk.ac.ebi.pride.gui.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
import uk.ac.ebi.pride.gui.data.Credentials;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;

import java.util.Base64;

import static uk.ac.ebi.pride.gui.task.GetPXSubmissionDetailTask.setProxyIfProvided;
import static uk.ac.ebi.pride.gui.util.Constant.TICKET_ID;

/**
 * Task for getting upload details from PRIDE web service
 *
 * @author Rui Wang
 * @version $Id$
 */
public class GetUploadDetailTask extends TaskAdapter<UploadDetail, String> {

    private static final Logger logger = LoggerFactory.getLogger(GetUploadDetailTask.class);

    private final RestTemplate restTemplate;
    private final UploadMethod method;
    private Credentials credentials;

    /**
     * Constructor
     *
     * @param userName user name
     * @param password password
     */
    public GetUploadDetailTask(UploadMethod method, String userName, char[] password) {
        this.restTemplate = new RestTemplate();
        this.credentials = new Credentials(userName, new String(password));
        this.method = method;
    }

    @Override
    protected UploadDetail doInBackground() throws Exception {

        DesktopContext context = App.getInstance().getDesktopContext();
        String baseUrl = context.getProperty("px.upload.detail.url");
        String reUploadUrl = context.getProperty("px.reupload.detail.url");
        String toolVersion = context.getProperty("px.submission.tool.version");

        UploadDetail uploadDetail = null;
        try {
            logger.info("Starting upload detail retrieval for method: {}", method.getMethod());
            logger.debug("Base URL: {}, Re-upload URL: {}, Tool version: {}", baseUrl, reUploadUrl, toolVersion);
            
            uploadDetail = ((AppContext)context).getSubmissionRecord().getUploadDetail();
            if(uploadDetail !=null){
                logger.info("Found existing upload detail in submission record");
                return uploadDetail;
            }

            setProxyIfProvided(restTemplate);
            logger.debug("Proxy configuration applied to RestTemplate");

            String credentials = this.credentials.getUsername() + ":" + this.credentials.getPassword();
            String base64Creds = Base64.getEncoder().encodeToString(credentials.getBytes());
            logger.debug("Credentials prepared for user: {}", this.credentials.getUsername());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Authorization", "Basic " + base64Creds);
            headers.add("version",toolVersion);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String ticketId = context.getProperty(TICKET_ID);
            
            if(ticketId!=null && !ticketId.equals("")) {
                logger.info("Attempting re-upload with ticket ID: {}", ticketId);
                logger.debug("Re-upload URL: {}", reUploadUrl);
                
                try {
                    uploadDetail = restTemplate.exchange(reUploadUrl,HttpMethod.GET,entity,UploadDetail.class,method.getMethod(),ticketId).getBody();
                    if(uploadDetail==null){
                        logger.error("Re-upload API call succeeded but returned null response. Ticket ID may be invalid or state may not be valid. URL: {}, Method: {}, Ticket ID: {}", reUploadUrl, method.getMethod(), ticketId);
                    } else {
                        logger.info("Re-upload details retrieved successfully");
                    }
                } catch (Exception e) {
                    logger.error("Exception during re-upload API call. URL: {}, Method: {}, Ticket ID: {}, Exception type: {}, Message: {}", 
                               reUploadUrl, method.getMethod(), ticketId, e.getClass().getSimpleName(), e.getMessage(), e);
                }
            } else {
                logger.info("No ticket ID found, attempting new upload");
                logger.debug("Base URL: {}", baseUrl);
                
                try {
                    uploadDetail = restTemplate.exchange(baseUrl, HttpMethod.GET, entity, UploadDetail.class, method.getMethod()).getBody();
                    if (uploadDetail == null || uploadDetail.getDropBox() == null) {
                        logger.error("New upload API call succeeded but returned null response. URL: {}, Method: {}", baseUrl, method.getMethod());
                    } else {
                        logger.info("New upload details retrieved successfully");
                    }
                } catch (Exception e) {
                    logger.error("Exception during new upload API call. URL: {}, Method: {}, Exception type: {}, Message: {}", 
                               baseUrl, method.getMethod(), e.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        } catch(Exception ex){
            logger.error("Unexpected error in upload detail retrieval. Exception type: {}, Message: {}", 
                       ex.getClass().getSimpleName(), ex.getMessage(), ex);
        }
        
        // If upload details are null, show warning message to user
        if (uploadDetail == null || uploadDetail.getDropBox() == null) {
            String warningMessage = "Failed to retrieve upload details for " + method.getMethod() + " upload method." +
                "\n\nThis could be due to:" +
                "\n• Network connectivity issues" +
                "\n• Server-side problems" +
                "\n• Invalid credentials" +
                "\n• Service temporarily unavailable" +
                "\n\nRecommended actions:" +
                "\n1. Go back and try selecting FTP upload instead" +
                "\n2. Check your internet connection" +
                "\n3. Try again in a few minutes" +
                "\n4. Use Globus for file transfer: https://www.ebi.ac.uk/pride/markdownpage/globus " +
                "\n\nIf the problem persists, contact PRIDE support.";
            
            logger.warn("Upload details are null, publishing warning message to user");
            publish(warningMessage);
        }
        
        return uploadDetail;
    }
}
