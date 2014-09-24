package uk.ac.ebi.pride.gui.task;

import org.apache.http.HttpHost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.user.ContactDetail;
import uk.ac.ebi.pride.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.web.util.template.SecureRestTemplateFactory;

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

    /**
     * Constructor
     *
     * @param userName pride user name
     * @param password pride password
     */
    public GetPrideUserDetailTask(String userName, char[] password) {
        this.restTemplate = SecureRestTemplateFactory.getTemplate(userName, new String(password));
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
        String baseUrl = context.getProperty("pride.user.detail.url");

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

            return restTemplate.getForObject(baseUrl, ContactDetail.class);
        } catch (Exception ex) {
            publish("Failed to login, please check user name or password");
        }

        return null;
    }
}

