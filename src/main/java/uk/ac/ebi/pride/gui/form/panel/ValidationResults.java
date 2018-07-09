package uk.ac.ebi.pride.gui.form.panel;

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
public class ValidationResults implements ActionListener {

  JEditorPane jEditorPane = new JEditorPane();
  JButton emailButton = new JButton("Ask support from PRIDE Team");
  JPanel buttonPanel = new JPanel();
  String htmlString;

  protected ValidationResults(String validationReport) {
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

    // add some styles to the html
    StyleSheet styleSheet = kit.getStyleSheet();
    styleSheet.addRule("body {color:#000; font-family:times; margin: 4px; }");
    styleSheet.addRule(".correct {background-color: #00897b;}"); // green
    styleSheet.addRule(".incorrect {background-color: #e53935;}"); // red
    styleSheet.addRule(".warning {background-color: #ffeb3b;}"); // yellow
    styleSheet.addRule("div {background-color: #00897b; text-align: center;}");
    styleSheet.addRule("h1, h3 {color: #ffffff;}");
    styleSheet.addRule("table {width: 100%; border-collapse: collapse;}");
    styleSheet.addRule("th, td {border: 1px solid black; padding: 10px; text-align: left;}");
    styleSheet.addRule("th{background-color:#00bcd4;}"); // cyan

    // create a document, set it on the jeditorpane, then add the html
    Document doc = kit.createDefaultDocument();
    jEditorPane.setDocument(doc);
    jEditorPane.setText(htmlString);

    // Set JFrame layout and other properties
    JFrame frame = new JFrame("PX Submission Tool Validation Report");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(pane, BorderLayout.CENTER);
    frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    frame.setSize(800, 600);
    frame.setVisible(true);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // if user pressed the emailButton button
    if (e.getSource().equals(emailButton)) {
      try {
        Mail.mailto(Mail.TO, Mail.SUBJECT, Mail.CONTENT);
      } catch (IOException | URISyntaxException ex) {
        Logger.getLogger(ValidationResults.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}
