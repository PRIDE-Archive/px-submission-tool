package uk.ac.ebi.pride.gui.form.panel;

import uk.ac.ebi.pride.gui.form.comp.ContextAwarePanel;
import uk.ac.ebi.pride.gui.util.Mail;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/** @author Suresh Hewapathirana */
public class ValidationReportFrame extends ContextAwarePanel implements ActionListener {

  JEditorPane jEditorPane = new JEditorPane();
  JButton emailButton = new JButton(appContext.getProperty("validation.report.help.button"));
  JPanel buttonPanel = new JPanel();
  String htmlString;
  JFrame frame;

  protected ValidationReportFrame(String validationReport) {
    this.htmlString = validationReport;
    initialize();
  }

  /** Initialise the JFrame */
  private void initialize() {

    emailButton.addActionListener(this);
    buttonPanel.add(emailButton);

    // add a HTMLEditorKit to the editor pane
    HTMLEditorKit kit = new HTMLEditorKit();
    jEditorPane.setEditorKit(kit);
    jEditorPane.setEditable(false);
    JScrollPane pane = new JScrollPane(jEditorPane);
    addStyles(kit.getStyleSheet());

    // create a document, set it on the JEditorPane, then add the html
    Document doc = kit.createDefaultDocument();
    jEditorPane.setDocument(doc);
    jEditorPane.setText(htmlString);

    // Set JFrame layout and other properties
    frame = new JFrame(appContext.getProperty("validation.report.frame.title"));
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(pane, BorderLayout.CENTER);
    frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    frame.setSize(800, 600);
//    frame.setVisible(true);
  }

  /**
   * Open the validation results window
   */
  protected void open(){
    frame.setVisible(true);
  }

  /**
   * Add CSS styles to the HTML
   * @param styleSheet Original styleSheet
   * @return StyleSheet with styles added
   */
  private StyleSheet addStyles(StyleSheet styleSheet){

    styleSheet.addRule("body {color:#000; font-family:times; margin: 4px; }");
    styleSheet.addRule("div {background-color: #00897b; text-align: center;}"); // green
    styleSheet.addRule("h1, h3 {color: #ffffff;}");
    styleSheet.addRule("table {width: 100%; border-collapse: collapse;}");
    styleSheet.addRule("th, td {border: 1px solid black; padding: 10px; text-align: left;}");
    styleSheet.addRule("th{background-color:#00897b;}"); // green
    styleSheet.addRule(".correct {background-color: #00897b;}"); // green
    styleSheet.addRule(".incorrect {background-color: #e53935;}"); // red
    styleSheet.addRule(".warning {background-color: #ffeb3b;}"); // yellow
    return styleSheet;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // if user pressed the emailButton button
    if (e.getSource().equals(emailButton)) {
      try {
        Mail.mailto(Mail.TO, Mail.SUBJECT, Mail.CONTENT);
      } catch (IOException | URISyntaxException ex) {
        Logger.getLogger(ValidationReportFrame.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}
