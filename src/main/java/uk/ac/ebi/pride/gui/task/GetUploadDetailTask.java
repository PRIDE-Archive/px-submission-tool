package uk.ac.ebi.pride.gui.task;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
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
            setProxyIfProvided(restTemplate);

            String credentials = this.credentials.getUsername() + ":" + this.credentials.getPassword();
            String base64Creds = Base64.getEncoder().encodeToString(credentials.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Authorization", "Basic " + base64Creds);
            headers.add("version",toolVersion);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String ticketId = context.getProperty(TICKET_ID);
            if(ticketId==null || ticketId.equals("") ) {
                uploadDetail = restTemplate.exchange(baseUrl, HttpMethod.GET, entity, UploadDetail.class, method.getMethod()).getBody();
            } else {
                uploadDetail = restTemplate.exchange(reUploadUrl,HttpMethod.GET,entity,UploadDetail.class,method.getMethod(),ticketId).getBody();
                if(uploadDetail==null){
                    publish("Error in getting re-upload details either ticketId is not valid or state is not valid");
                }
            }

        } catch(Exception ex){
            publish("Error in getting upload details");
        }
        return uploadDetail;
    }
}
