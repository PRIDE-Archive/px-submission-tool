package uk.ac.ebi.pride.gui.task;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
import uk.ac.ebi.pride.gui.data.Credentials;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Base64;
import java.util.Properties;

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
        String toolVersion = context.getProperty("px.submission.tool.version");

        UploadDetail uploadDetail = null;
        try {
            Properties props = System.getProperties();
            String proxyHost = props.getProperty("http.proxyHost");
            String proxyPort = props.getProperty("http.proxyPort");

            if (proxyHost != null && proxyPort != null) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
                SimpleClientHttpRequestFactory requestFactory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
                requestFactory.setProxy(proxy);
            }

            String credentials = this.credentials.getUsername() + ":" + this.credentials.getPassword();
            String base64Creds = Base64.getEncoder().encodeToString(credentials.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Authorization", "Basic " + base64Creds);
            headers.add("version",toolVersion);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            uploadDetail = restTemplate.exchange(baseUrl, HttpMethod.GET, entity, UploadDetail.class, method.getMethod()).getBody();

        } catch(Exception ex){
            publish("Error in getting upload details");
        }
        return uploadDetail;
    }
}
