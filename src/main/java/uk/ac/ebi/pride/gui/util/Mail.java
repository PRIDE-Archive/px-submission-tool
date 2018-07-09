package uk.ac.ebi.pride.gui.util;


import java.awt.Desktop;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

/**
 * This class is responsible loading email data to a new email composed
 * from default email client
 *
 * @author hewapathirana
 */
public class Mail {

  public static final String TO = "pride-support@ebi.ac.uk";
  public static final String SUBJECT = "PX Submission Validation Report Error";
  public static final String CONTENT = "Hello,\n\n"
          + "Please investigate the validation error(s) and find the log file attached herewith.\n"
          + "===================================================================================\n"
          + "Please attach the log file log/px_submission.log to the email and delete this text\n"
          + "===================================================================================\n\n"
          + "Regards,";

  /**
   * Create a mailto URL and load data into default email client as a new mail
   *
   * @param recipients To whom this email sent
   * @param subject Email Subject
   * @param body Email Body as a plain text
   * @throws IOException
   * @throws URISyntaxException
   */
  public static void mailto(String recipients, String subject, String body) throws IOException, URISyntaxException {
    String uriString = String.format("mailto:%s?subject=%s&body=%s",
            recipients,
            urlEncode(subject),
            urlEncode(body));
    Desktop.getDesktop().browse(new URI(uriString));
  }

  /**
   * Encode URL String with UTF-8
   *
   * @param urlString URL
   * @return Encoded EUL as a String
   */
  private static final String urlEncode(String urlString) {
    try {
      return URLEncoder.encode(urlString, "UTF-8").replace("+", "%20");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}