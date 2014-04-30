package uk.ac.ebi.pride.gui.task;

import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadDetail;
import uk.ac.ebi.pride.archive.submission.model.submission.UploadMethod;
import uk.ac.ebi.pride.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.web.util.template.SecureRestTemplateFactory;

/**
 * Task for getting upload details from PRIDE web service
 *
 * @author Rui Wang
 * @version $Id$
 */
public class GetUploadDetailTask extends TaskAdapter<UploadDetail, String> {

    private final RestTemplate restTemplate;
    private final UploadMethod method;

    /**
     * Constructor
     *
     * @param userName user name
     * @param password password
     */
    public GetUploadDetailTask(UploadMethod method, String userName, char[] password) {
        this.restTemplate = SecureRestTemplateFactory.getTemplate(userName, new String(password));
        this.method = method;
    }

    @Override
    protected UploadDetail doInBackground() throws Exception {

        DesktopContext context = App.getInstance().getDesktopContext();
        String baseUrl = context.getProperty("px.upload.detail.url");

        return restTemplate.getForObject(baseUrl, UploadDetail.class, method.getMethod());
    }
}
