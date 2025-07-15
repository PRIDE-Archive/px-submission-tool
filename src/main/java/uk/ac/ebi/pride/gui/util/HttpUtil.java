package uk.ac.ebi.pride.gui.util;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.io.IOException;

/**
 * Http utility class
 * <p/>
 * User: rwang
 * Date: 20-Aug-2010
 * Time: 12:53:03
 */
public class HttpUtil {

    public static void openURL(String url) {
        String osName = System.getProperty("os.name");
        try {
            if (osName.startsWith("Mac OS")) {
                Desktop.getDesktop().browse(URI.create(url));
            } else if (osName.startsWith("Windows")) {
                // Use ProcessBuilder for Windows
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", url);
                pb.start();
            } else { //assume Unix or Linux
                String[] browsers = {"firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape"};
                String browser = null;

                for (String browserName : browsers) {
                    ProcessBuilder pb = new ProcessBuilder("which", browserName);
                    if (pb.start().waitFor() == 0) {
                        browser = browserName;
                        break;
                    }
                }

                if (browser == null) {
                    throw new IOException("Could not find web browser");
                } else {
                    ProcessBuilder pb = new ProcessBuilder(browser, url);
                    pb.start();
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
        }
    }
}
