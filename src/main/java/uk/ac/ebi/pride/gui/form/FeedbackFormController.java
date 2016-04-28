package uk.ac.ebi.pride.gui.form;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.gui.GUIUtilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.form
 * Timestamp: 2016-04-27 15:31
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * ---
 * This controller is in charge of the Feedback sub-form within the submission form
 */
public class FeedbackFormController extends Form {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionForm.class);

    // Submission data
    private String submissionRef;

    // Form components
    JPanel feedbackFramePanel;
    JPanel feedbackMainPanel;
    JPanel ratingPanel;
    JPanel additionalFeedbackInfoPanel;

    public FeedbackFormController(String subRef) {
        submissionRef = subRef;
        initComponents();
    }

    private void initComponents() {
// Feedback Frame panel providing panel position and padding
        feedbackFramePanel = new JPanel(new BorderLayout());
        feedbackFramePanel.setBorder(new EmptyBorder(20, 0, 10, 0));
        feedbackFramePanel.setPreferredSize(new Dimension(500, 200));

        // Feedback Main panel, contains the actual feedback components
        feedbackMainPanel = new JPanel(new BorderLayout());
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
        ratingPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
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
        additionalFeedbackInfoPanel = new JPanel(new BorderLayout());
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
    }

    public void addToParentForm(Form form, Object constraints) {
        form.add(feedbackFramePanel, constraints);
        form.revalidate();
        form.repaint();
    }
}
