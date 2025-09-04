package uk.ac.ebi.pride.gui.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.dataprovider.utils.SubmissionTypeConstants;
import uk.ac.ebi.pride.archive.submission.model.project.ProjectDetail;
import uk.ac.ebi.pride.archive.submission.model.project.ProjectDetailList;
import uk.ac.ebi.pride.gui.data.Credentials;
import uk.ac.ebi.pride.gui.util.Constant;

import javax.swing.*;
import java.awt.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.regex.Matcher;

/**
 * Task for get PX submission details using given user name and password
 *
 * @author Rui Wang
 * @version $Id$
 */
public class GetPXSubmissionDetailTask extends AbstractWebServiceTask<HashMap<String, SubmissionTypeConstants>> {
    private static final Logger logger = LoggerFactory.getLogger(GetPXSubmissionDetailTask.class);

    private final RestTemplate restTemplate;

    private Credentials credentials;

    public GetPXSubmissionDetailTask(String userName, String password) {
        this.restTemplate = new RestTemplate();
        this.credentials = new Credentials(userName, new String(password));
    }

    @Override
    protected HashMap<String,SubmissionTypeConstants> doInBackground() throws Exception {
        HashMap<String,SubmissionTypeConstants> pxAndPadAccessions = new LinkedHashMap<>();
        
        String detailUrl = App.getInstance().getDesktopContext().getProperty("px.submission.detail.url");
        logger.info("Starting PX submission detail retrieval for user: {}", credentials.getUsername());
        logger.debug("Detail URL: {}", detailUrl);

        try {
            setProxyIfProvided(restTemplate);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Credentials> entity = new HttpEntity<>(credentials, headers);
            logger.debug("Request headers prepared with content type: {}", headers.getContentType());
            
            logger.info("Making API call to retrieve project details");
            ProjectDetailList projectDetailList = restTemplate.exchange(detailUrl, HttpMethod.POST, entity, ProjectDetailList.class).getBody();
            
            if (projectDetailList == null) {
                logger.error("API call succeeded but returned null ProjectDetailList. URL: {}", detailUrl);
                return pxAndPadAccessions;
            }
            
            if (projectDetailList.getProjectDetails() == null) {
                logger.error("ProjectDetailList contains null project details. URL: {}", detailUrl);
                return pxAndPadAccessions;
            }
            
            logger.info("Retrieved {} project details", projectDetailList.getProjectDetails().size());
            
            for (ProjectDetail projectDetail : projectDetailList.getProjectDetails()) {
                if (projectDetail == null) {
                    logger.warn("Encountered null ProjectDetail in list");
                    continue;
                }
                
                String accession = projectDetail.getAccession();
                if (accession == null) {
                    logger.warn("ProjectDetail has null accession");
                    continue;
                }
                
                Matcher pxMatcher = Constant.PX_ACC_PATTERN.matcher(accession);
                Matcher padMatcher = Constant.PAD_ACC_PATTERN.matcher(accession);
                if (pxMatcher.matches()) {
                    pxAndPadAccessions.put(accession,projectDetail.getSubmissionType());
                    logger.debug("Added PX accession: {}", accession);
                }
                if (padMatcher.matches()) {
                    pxAndPadAccessions.put(accession,projectDetail.getSubmissionType());
                    logger.debug("Added PAD accession: {}", accession);
                }
            }
            
            logger.info("Successfully processed {} accessions", pxAndPadAccessions.size());
            
        } catch (Exception ex) {
            logger.error("Failed to retrieve project details. URL: {}, Exception type: {}, Message: {}", 
                       detailUrl, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            
            Runnable eventDispatcher = () -> {
                // show warning dialog
                App app = (App) App.getInstance();
                JOptionPane.showConfirmDialog(app.getMainFrame(),
                        app.getDesktopContext().getProperty("pride.login.resubmission.error.message"),
                        app.getDesktopContext().getProperty("pride.login.error.title"),
                        JOptionPane.CLOSED_OPTION, JOptionPane.WARNING_MESSAGE);
            };
            EventQueue.invokeLater(eventDispatcher);
        }

        return pxAndPadAccessions;
    }

    public static void setProxyIfProvided(RestTemplate restTemplate) {
        Properties props = System.getProperties();
        String proxyHost = props.getProperty("http.proxyHost");
        String proxyPort = props.getProperty("http.proxyPort");

        if (proxyHost != null && proxyPort != null) {
            logger.info("Using proxy server {} and port {}", proxyHost, proxyPort);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
            SimpleClientHttpRequestFactory requestFactory = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
            requestFactory.setProxy(proxy);
        }
    }
}
