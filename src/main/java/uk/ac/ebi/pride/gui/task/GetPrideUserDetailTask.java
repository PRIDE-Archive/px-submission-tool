package uk.ac.ebi.pride.gui.task;

import org.apache.http.HttpHost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.user.ContactDetail;
import uk.ac.ebi.pride.gui.data.Credentials;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;

import java.util.Properties;

/**
 * GetPrideUserDetailTask retrieves pride user details using pride web service
 *
 * @author Rui Wang
 * @version $Id$
 */
public class GetPrideUserDetailTask extends TaskAdapter<ContactDetail, String> {

    private static final Logger logger = LoggerFactory.getLogger(GetPrideUserDetailTask.class);

    private final RestTemplate restTemplate;

    public final Credentials userCredentials;

    /**
     * Constructor
     *
     * @param userName pride user name
     * @param password pride password
     */
    public GetPrideUserDetailTask(String userName, char[] password) {
        userCredentials = new Credentials(userName, new String(password));
        this.restTemplate = new RestTemplate();
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     * <p/>
     * <p/>
     * Note that this method is executed only once.
     * <p/>
     * <p/>
     * Note: this method is executed in a background thread.
     *
     * @return the computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    protected ContactDetail doInBackground() throws Exception {
        DesktopContext context = App.getInstance().getDesktopContext();
        String userTokenUrl = context.getProperty("px.user.token.url");
        String userDetailUrl = context.getProperty("px.user.detail.url");

        try {
            // set proxy
            Properties props = System.getProperties();
            String proxyHost = props.getProperty("http.proxyHost");
            String proxyPort = props.getProperty("http.proxyPort");

            if (proxyHost != null && proxyPort != null) {
                logger.info("Using proxy server {} and port {}", proxyHost, proxyPort);
                HttpComponentsClientHttpRequestFactory factory = ((HttpComponentsClientHttpRequestFactory) restTemplate.getRequestFactory());
                DefaultHttpClient defaultHttpClient = (DefaultHttpClient) factory.getHttpClient();
                HttpHost proxy = new HttpHost(proxyHost.trim(), Integer.parseInt(proxyPort));
                defaultHttpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String requestBody = userCredentials.toString();
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            final String token = restTemplate.exchange(userTokenUrl, HttpMethod.POST, entity, String.class).getBody();

            headers.set("Authorization", "Bearer " + token);
            entity = new HttpEntity<>(headers);
            return restTemplate.exchange(userDetailUrl, HttpMethod.GET, entity, ContactDetail.class).getBody();
        } catch (ResourceAccessException resourceAccessException) {
            publish("Proxy/Firewall issue");
        } catch (HttpServerErrorException ex) {
            publish("UserCredentials mismatch");
        }

        return null;
    }
}

