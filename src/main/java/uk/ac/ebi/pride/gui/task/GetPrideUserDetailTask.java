package uk.ac.ebi.pride.gui.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.user.ContactDetail;
import uk.ac.ebi.pride.gui.data.Credentials;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.toolsuite.gui.task.TaskAdapter;

import static uk.ac.ebi.pride.gui.task.GetPXSubmissionDetailTask.setProxyIfProvided;

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
        String userLogin = context.getProperty("px.user.login.url");
        String toolVersion = context.getProperty("px.submission.tool.version");

        try {
            // set proxy
            setProxyIfProvided(restTemplate);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("version",toolVersion);
            HttpEntity<Credentials> entity = new HttpEntity<>(userCredentials, headers);
            return restTemplate.exchange(userLogin, HttpMethod.POST, entity, ContactDetail.class).getBody();
        } catch (ResourceAccessException resourceAccessException) {
            publish("Proxy/Firewall issue");
        } catch (HttpClientErrorException ex) {
            publish("UserCredentials mismatch");
        }

        return null;
    }
}

