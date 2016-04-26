package uk.ac.ebi.pride.gui.form;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.form.comp.HeaderPanel;
import uk.ac.ebi.pride.gui.util.BorderUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
    private JPanel progPanel;


    public SubmissionForm() {
        initComponents();
    }

    private void initComponents() {
        // setup the main pane
        this.setLayout(new BorderLayout());

        // add gap
        this.add(Box.createRigidArea(new Dimension(20, 150)), BorderLayout.NORTH);

        // create progress panel
        this.progPanel = createProgressPanel();
        this.add(this.progPanel, BorderLayout.CENTER);
    }

    /**
     * Create the progress bar panel
     *
     * @return JPanel  panel to show the progress of the upload
     */
    private JPanel createProgressPanel() {
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // uploading icon
        Icon icon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("upload.large.icon"));
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
        progressBar.setValue(0);
        progressDetailPanel.add(progressBar, c);

        // control button
        c.gridx = 1;
        c.gridy = 1;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        Icon cancelIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("pause.small.icon"));
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
        Icon cancelIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("pause.small.icon"));
        controlButton.setIcon(cancelIcon);
        controlButton.setEnabled(enabled);
        controlButton.setActionCommand(STOP_SUBMISSION_PROP);
    }

    public void enableStartButton(boolean enabled) {
        Icon startIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("start.small.icon"));
        controlButton.setIcon(startIcon);
        controlButton.setEnabled(enabled);
        controlButton.setActionCommand(START_SUBMISSION_PROP);
    }

    public void enabledSuccessButton(boolean enabled) {
        Icon successIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("success.small.icon"));
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

        // Show feeback capture panel
        this.showFeedbackMessage(submissionRef);
    }

    // Feedback submission feature

    public void showFeedbackMessage(String submissionRef) {
        // Put the upload bar on the northern border
        this.remove(this.progPanel);
        this.add(this.progPanel, BorderLayout.NORTH);

        // Feedback Frame panel providing panel position and padding
        JPanel feedbackFramePanel = new JPanel(new BorderLayout());
        feedbackFramePanel.setBorder(new EmptyBorder(20, 0, 10, 0));
        feedbackFramePanel.setPreferredSize(new Dimension(500, 200));

        // Feedback Main panel, contains the actual feedback components
        JPanel feedbackMainPanel = new JPanel(new BorderLayout());
        //feedbackMainPanel.setBackground(Color.LIGHT_GRAY);

        // Title for feedback
        // WARNING - This component has been removed from the final version of the panel
        // JLabel formTitle = new JLabel();
        // formTitle.setBorder(new EmptyBorder(5, 10, 0, 0));
        // formTitle.setHorizontalAlignment(SwingConstants.LEFT);
        // formTitle.setVerticalAlignment(SwingConstants.TOP);
        // formTitle.setFont(formTitle.getFont().deriveFont(16f));
        // formTitle.setText(App.getInstance().getDesktopContext().getProperty("feedback.form.title"));
        // Add it to the main panel
        //feedbackMainPanel.add(formTitle, BorderLayout.NORTH);

        // Rating Panel
        JPanel ratingPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        ratingPanel.setBorder(new EmptyBorder(5, 5, 0, 5));
        ratingPanel.setOpaque(false);
        // Smiley buttons
        // Very bad experience
        Icon veryBadExperienceIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.very_bad.icon"));
        JRadioButton veryBadExperienceButton = new JRadioButton(veryBadExperienceIcon);
        veryBadExperienceButton.setOpaque(false);
        veryBadExperienceButton.setToolTipText(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.very_bad.tooltip"));
        // Bad experience
        Icon badExperienceIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.bad.icon"));
        JRadioButton badExperienceButton = new JRadioButton(badExperienceIcon);
        badExperienceButton.setToolTipText(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.bad.tooltip"));
        badExperienceButton.setOpaque(false);
        // Neutral experience
        Icon neutralExperienceIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.neutral.icon"));
        JRadioButton neutralExperienceButton = new JRadioButton(neutralExperienceIcon);
        neutralExperienceButton.setToolTipText(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.neutral.tooltip"));
        neutralExperienceButton.setOpaque(false);
        neutralExperienceButton.setSelected(true);
        // Good experience
        Icon goodExperienceIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.good.icon"));
        JRadioButton goodExperienceButton = new JRadioButton(goodExperienceIcon);
        goodExperienceButton.setToolTipText(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.good.tooltip"));
        goodExperienceButton.setOpaque(false);
        // Very good experience
        Icon veryGoodExperienceIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.very_good.icon"));
        JRadioButton veryGoodExperienceButton = new JRadioButton(veryGoodExperienceIcon);
        veryGoodExperienceButton.setToolTipText(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.very_good.tooltip"));
        veryGoodExperienceButton.setOpaque(false);
        // Add buttons to the panel
        ratingPanel.add(veryBadExperienceButton);
        ratingPanel.add(badExperienceButton);
        ratingPanel.add(neutralExperienceButton);
        ratingPanel.add(goodExperienceButton);
        ratingPanel.add(veryGoodExperienceButton);
        // Radio buttons group
        ButtonGroup ratingButtonsGroup = new ButtonGroup();
        ratingButtonsGroup.add(veryBadExperienceButton);
        ratingButtonsGroup.add(badExperienceButton);
        ratingButtonsGroup.add(neutralExperienceButton);
        ratingButtonsGroup.add(goodExperienceButton);
        ratingButtonsGroup.add(veryGoodExperienceButton);
        // TEST
        //veryBadExperienceButton.setBorderPainted(true);
        //veryBadExperienceButton.setBorder(BorderFactory.createLoweredBevelBorder());
        // Add rating panel to main panel
        feedbackMainPanel.add(ratingPanel, BorderLayout.CENTER);

        // Additional comments
        JPanel additionalFeedbackInfoPanel = new JPanel(new BorderLayout());
        additionalFeedbackInfoPanel.setBorder(new EmptyBorder(0, 5, 15, 5));
        additionalFeedbackInfoPanel.setOpaque(false);
        // Text box
        JTextArea feedbackAdditionalInfoText = new JTextArea(7, 1);
        feedbackAdditionalInfoText.setLineWrap(true);
        feedbackAdditionalInfoText.setWrapStyleWord(true);
        JScrollPane fbAdditionalInfoScrollPane = new JScrollPane(feedbackAdditionalInfoText);
        //fbAdditionalInfoScrollPane.setAutoscrolls(true);
        // Attach elements to panel
        additionalFeedbackInfoPanel.add(fbAdditionalInfoScrollPane, BorderLayout.CENTER);
        // Add additional feedback panel to main feedback panel
        feedbackMainPanel.add(additionalFeedbackInfoPanel, BorderLayout.SOUTH);

        // Add main panel to frame panel
        feedbackFramePanel.add(feedbackMainPanel, BorderLayout.CENTER);
        this.add(feedbackFramePanel, BorderLayout.CENTER);
        this.revalidate();
        this.repaint();
    }
}
