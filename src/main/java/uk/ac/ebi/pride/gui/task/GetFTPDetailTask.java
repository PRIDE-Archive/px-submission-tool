package uk.ac.ebi.pride.gui.task;

import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.prider.webservice.submission.model.FtpUploadDetail;
import uk.ac.ebi.pride.web.util.template.SecureRestTemplateFactory;

/**
 * Task for getting FTP upload details from PRIDE web service
 *
 * @author Rui Wang
 * @version $Id$
 */
public class GetFTPDetailTask extends TaskAdapter<FtpUploadDetail, String> {

    private final RestTemplate restTemplate;

    /**
     * Constructor
     *
     * @param userName user name
     * @param password password
     */
    public GetFTPDetailTask(String userName, char[] password) {
        this.restTemplate = SecureRestTemplateFactory.getTemplate(userName, new String(password));
    }

    @Override
    protected FtpUploadDetail doInBackground() throws Exception {

        DesktopContext context = App.getInstance().getDesktopContext();
        String baseUrl = context.getProperty("px.ftp.detail.url");

        return restTemplate.getForObject(baseUrl, FtpUploadDetail.class);
    }
}
