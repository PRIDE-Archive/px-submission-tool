package uk.ac.ebi.pride.gui.form;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.form.comp.HeaderPanel;
import uk.ac.ebi.pride.gui.util.BorderUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

/**
 * This form is responsible for performing the submission and monitoring the progress
 *
 * @author Rui Wang
 * @version $Id$
 */
public class SubmissionForm extends Form implements ActionListener {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionForm.class);

    /**
     * Action commands & properties
     */
    public static final String START_SUBMISSION_PROP = "startAction";
    public static final String STOP_SUBMISSION_PROP = "stopAction";

    private JLabel uploadingFileLabel;
    private JLabel progressLabel;
    private JProgressBar progressBar;
    private JButton controlButton;


    public SubmissionForm() {
        initComponents();
    }

    private void initComponents() {
        // setup the main pane
        this.setLayout(new BorderLayout());

        // add gap
        this.add(Box.createRigidArea(new Dimension(20, 150)), BorderLayout.NORTH);

        // create progress panel
        JPanel progressPanel = createProgressPanel();
        this.add(progressPanel, BorderLayout.CENTER);
    }

    /**
     * Create the progress bar panel
     *
     * @return JPanel  panel to show the progress of the upload
     */
    private JPanel createProgressPanel() {
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // uploading icon
        Icon icon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("ftp.upload.large.icon"));
        JLabel iconLabel = new JLabel(icon);
        progressPanel.add(iconLabel);

        // progress detail panel
        JPanel progressDetailPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;

        // label to indicate which file is uploading
        c.gridx = 0;
        c.gridy = 0;

        uploadingFileLabel = new JLabel();
        uploadingFileLabel.setFont(uploadingFileLabel.getFont().deriveFont(Font.BOLD).deriveFont(13f));
        progressDetailPanel.add(uploadingFileLabel, c);

        // progress bar
        c.gridx = 0;
        c.gridy = 1;

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(620, 25));
        progressBar.setValue(1);
        progressDetailPanel.add(progressBar, c);

        // control button
        c.gridx = 1;
        c.gridy = 1;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        Icon cancelIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("ftp.cancel.small.icon"));
        controlButton = GUIUtilities.createLabelLikeButton(cancelIcon, null);
        controlButton.setActionCommand(STOP_SUBMISSION_PROP);
        controlButton.addActionListener(this);
        buttonPanel.add(controlButton);
        progressDetailPanel.add(buttonPanel, c);

        // label to indicate the upload progress
        c.gridx = 0;
        c.gridy = 2;
        progressLabel = new JLabel();
        progressDetailPanel.add(progressLabel, c);

        progressPanel.add(progressDetailPanel);

        return progressPanel;
    }

    /**
     * Set upload message
     * Note: this message normally used to indicate overall state of the upload
     *
     * @param message upload message
     */
    public void setUploadMessage(String message) {
        uploadingFileLabel.setText(message);
    }

    /**
     * Set progress message
     * @param message   progress message
     */
    public void setProgressMessage(String message) {
        progressLabel.setText(message);
    }

    /**
     * Set the status of the progress bar
     *
     * @param bytesToTransfer  total bytes to be transferred
     * @param bytesTransferred number of bytes already been transferred
     */
    public void setProgress(long bytesToTransfer, long bytesTransferred, int totalNumOfFiles, int uploadedNumOfFiles) {
        int percentage = Math.round((bytesTransferred * 1.0f / bytesToTransfer) * 100);
        // set progress bar
        progressBar.setValue(percentage);

        // set progress label
        String megTransferred = NumberFormat.getInstance().format(bytesTransferred / (1024 * 1024));
        String megToTransfer = NumberFormat.getInstance().format(bytesToTransfer / (1024 * 1024));
        String progressLabelText = percentage + "% completed - " + megTransferred + " of " + megToTransfer +
                                    " MB [" + uploadedNumOfFiles + " of " + totalNumOfFiles + " files]";
        progressLabel.setText(progressLabelText);
    }

    public void enableCancelButton(boolean enabled) {
        Icon cancelIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("ftp.cancel.small.icon"));
        controlButton.setIcon(cancelIcon);
        controlButton.setEnabled(enabled);
        controlButton.setActionCommand(STOP_SUBMISSION_PROP);
    }

    public void enableStartButton(boolean enabled) {
        Icon startIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("ftp.start.small.icon"));
        controlButton.setIcon(startIcon);
        controlButton.setEnabled(enabled);
        controlButton.setActionCommand(START_SUBMISSION_PROP);
    }

    public void enabledSuccessButton(boolean enabled) {
        Icon successIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("ftp.success.small.icon"));
        controlButton.setIcon(successIcon);
        controlButton.setEnabled(enabled);
        controlButton.setActionCommand(null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String evtCmd = e.getActionCommand();
        if (evtCmd.equals(START_SUBMISSION_PROP)) {
            logger.debug("Restart button pressed");
            firePropertyChange(START_SUBMISSION_PROP, false, true);
        } else if (evtCmd.equals(STOP_SUBMISSION_PROP)) {
            logger.debug("Cancel button pressed");
            firePropertyChange(STOP_SUBMISSION_PROP, false, true);
        }
    }

    /**
     * Show submission complete message
     *
     * @param submissionRef submission reference id
     */
    public void showCompletionMessage(String submissionRef) {
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBorder(BorderUtil.createLoweredBorder());
        messagePanel.setPreferredSize(new Dimension(500, 200));

        JPanel contentPanel = new HeaderPanel();
        contentPanel.setLayout(new BorderLayout());

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setOpaque(false);
        JLabel titleLabel = new JLabel();
        Icon icon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("submission.complete.large.icon"));
        titleLabel.setIcon(icon);
        titleLabel.setForeground(Color.white);
        titleLabel.setFont(titleLabel.getFont().deriveFont(18f).deriveFont(Font.BOLD));
        titleLabel.setText(App.getInstance().getDesktopContext().getProperty("submission.complete.title"));

        JLabel refLabel = new JLabel(submissionRef);
        refLabel.setForeground(Color.green);
        refLabel.setFont(refLabel.getFont().deriveFont(18f).deriveFont(Font.BOLD));

        titlePanel.add(titleLabel);
        titlePanel.add(Box.createRigidArea(new Dimension(5, 5)));
        titlePanel.add(refLabel);

        JLabel messageLabel = new JLabel();
        messageLabel.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 0));
        messageLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        messageLabel.setVerticalTextPosition(SwingConstants.TOP);
        messageLabel.setForeground(Color.white);
        messageLabel.setFont(messageLabel.getFont().deriveFont(16f));
        messageLabel.setText(App.getInstance().getDesktopContext().getProperty("submission.complete.message"));

        contentPanel.add(titlePanel, BorderLayout.NORTH);
        contentPanel.add(messageLabel, BorderLayout.CENTER);


        messagePanel.add(contentPanel, BorderLayout.CENTER);
        this.add(messagePanel, BorderLayout.SOUTH);
        this.revalidate();
        this.repaint();
    }
}
