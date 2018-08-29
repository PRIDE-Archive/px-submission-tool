package uk.ac.ebi.pride.gui.form.dialog;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * @author Suresh Hewapathirana
 */
public class ValidationProgressDialog extends JDialog implements ActionListener {
  private static final String CANCEL_ACTION = "Cancel";

  public boolean shouldCancel = false;

  public ValidationProgressDialog(JFrame parent, String title, String message) {
    super(parent, title, true);

    setTitle("Validation on Progress...");
    setSize(500, 150);
    setResizable(false);

    setLayout(new BorderLayout());

    // Create components
    JButton buttonCancel = new JButton("Cancel");
    buttonCancel.setActionCommand(CANCEL_ACTION);
    buttonCancel.addActionListener(this);

    JLabel labelInfo = new JLabel("Info:");

    JProgressBar progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);
    progressBar.setString("");
    progressBar.setStringPainted(true);
    progressBar.setBorderPainted(true);

    JPanel bottomPanel = new JPanel();
    bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    bottomPanel.setLayout(new BorderLayout());
    bottomPanel.add(buttonCancel, BorderLayout.EAST);

    JPanel topPanel = new JPanel();
    topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    topPanel.setLayout(new BorderLayout());
    topPanel.add(labelInfo);

    JPanel centerPanel = new JPanel();
    centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    centerPanel.setLayout(new BorderLayout());
    centerPanel.add(progressBar);

    // And JPanel needs to be added to the JFrame itself!
    this.getContentPane().add(topPanel, BorderLayout.NORTH);
    this.getContentPane().add(centerPanel, BorderLayout.CENTER);
    this.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

    pack();
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setVisible(false);
  }

  public void actionPerformed(ActionEvent e) {
    String actCmd = e.getActionCommand();
    if (CANCEL_ACTION.equals(actCmd)) {
      shouldCancel = true;
      this.dispose();
    }
  }
}