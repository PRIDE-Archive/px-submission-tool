package uk.ac.ebi.pride.gui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.toolsuite.gui.desktop.Desktop;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Check whether there is a new update
 * <p/>
 * User: rwang
 * Date: 11-Nov-2010
 * Time: 17:19:36
 */
public class UpdateChecker {

    public static final Logger logger = LoggerFactory.getLogger(UpdateChecker.class);

    private static final Pattern VERSION_PATTERN = Pattern.compile(".*href=\"([\\d\\.]+)\\/.*");

    private final String updateUrl;

    private static String toolCurrentVersion;
    private static String latestOrHigherVersion;


    public UpdateChecker(String updateUrl) {
        this.updateUrl = updateUrl;
    }

    /**
     * Check whether there is a new update
     *
     * @return boolean return true if there is a new update.
     */
    public boolean hasUpdate(String currentVersion) {
        toolCurrentVersion = currentVersion;
        boolean toUpdate = false;
        BufferedReader reader = null;
        try {
            URL url = URI.create(updateUrl).toURL();
            int response = ((HttpURLConnection) url.openConnection()).getResponseCode();  // connect to the url
            if (response != 404) {
                // parse the web page
                reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = VERSION_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String version = matcher.group(1);
                        if (isHigherVersion(currentVersion, version)) {
                            latestOrHigherVersion = version;
                            currentVersion = version;
                            toUpdate = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to check for updates", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.warn("Failed to check for updates");
                }
            }
        }
        return toUpdate;
    }

    private boolean isHigherVersion(String currentVersion, String version) {
        String[] parts = currentVersion.split("-");
        String[] currentVersionNumbers = parts[0].split("\\.");
        String[] versionNumbers = version.split("\\.");
        for (int i = 0; i < currentVersionNumbers.length; i++) {
            int currentVersionNumber = Integer.parseInt(currentVersionNumbers[i]);
            int versionNumber = Integer.parseInt(versionNumbers[i]);
            if (versionNumber > currentVersionNumber) {
                return true;
            } else if (versionNumber < currentVersionNumber) {
                break;
            }
        }
        return false;
    }

    /**
     * Show update dialog
     */
    public static void showUpdateDialog() {
        int option = JOptionPane.showConfirmDialog(null, "<html><b>A new version of ProteomeXchange submission tool is available</b>.<br><br> " +
                "Please download latest version by clicking yes </html>", "Update Info", JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            DesktopContext context = Desktop.getInstance().getDesktopContext();
            String website = context.getProperty("px.submission.tool.url");
            HttpUtil.openURL(website);
        }

        String latestOrHigherVersionParts[] = latestOrHigherVersion.split("\\.");
        String toolCurrentVersionParts[] = toolCurrentVersion.split("\\.");

        if (Integer.parseInt(latestOrHigherVersionParts[0]) > Integer.parseInt(toolCurrentVersionParts[0])
                || Integer.parseInt(latestOrHigherVersionParts[1]) > Integer.parseInt(toolCurrentVersionParts[1])) {
            System.exit(0);
        }
    }
}
