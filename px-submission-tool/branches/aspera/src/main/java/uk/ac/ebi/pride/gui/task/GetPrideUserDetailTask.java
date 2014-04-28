package uk.ac.ebi.pride.gui.task;

import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.prider.webservice.user.model.ContactDetail;
import uk.ac.ebi.pride.web.util.template.SecureRestTemplateFactory;

/**
 * GetPrideUserDetailTask retrieves pride user details using pride web service
 *
 * @author Rui Wang
 * @version $Id$
 */
public class GetPrideUserDetailTask extends TaskAdapter<ContactDetail, String> {

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
            return restTemplate.getForObject(baseUrl, ContactDetail.class);
        } catch (Exception ex) {
            publish("Failed to login, please check user name or password");
        }

        return null;
    }
}

