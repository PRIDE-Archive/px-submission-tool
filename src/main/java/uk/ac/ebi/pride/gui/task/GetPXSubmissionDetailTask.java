package uk.ac.ebi.pride.gui.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.project.ProjectDetail;
import uk.ac.ebi.pride.archive.submission.model.project.ProjectDetailList;
import uk.ac.ebi.pride.web.util.template.SecureRestTemplateFactory;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Task for get PX submission details using given user name and password
 *
 * @author Rui Wang
 * @version $Id$
 */
public class GetPXSubmissionDetailTask extends AbstractWebServiceTask<Set<String>> {
    private static final Logger logger = LoggerFactory.getLogger(GetPXSubmissionDetailTask.class);

    private final RestTemplate restTemplate;

    public GetPXSubmissionDetailTask(String userName, String password) {
        this.restTemplate = SecureRestTemplateFactory.getTemplate(userName, password);
    }

    @Override
    protected Set<String> doInBackground() throws Exception {
        Set<String> pxAccessions = new LinkedHashSet<String>();

        String baseUrl = App.getInstance().getDesktopContext().getProperty("px.submission.detail.url");

        try {
            ProjectDetailList projectDetailList = restTemplate.getForObject(baseUrl, ProjectDetailList.class);

            for (ProjectDetail projectDetail : projectDetailList.getProjectDetails()) {
                pxAccessions.add(projectDetail.getAccession());
            }
        } catch (Exception ex) {
            logger.warn("Failed to login to retrieve project details", ex);
            Runnable eventDispatcher = new Runnable() {
                public void run() {
                    // show warning dialog
                    App app = (App) App.getInstance();
                    JOptionPane.showConfirmDialog(app.getMainFrame(),
                            app.getDesktopContext().getProperty("pride.login.resubmission.error.message"),
                            app.getDesktopContext().getProperty("pride.login.error.title"),
                            JOptionPane.CLOSED_OPTION, JOptionPane.WARNING_MESSAGE);
                }
            };
            EventQueue.invokeLater(eventDispatcher);
        }

        return pxAccessions;
    }
}
