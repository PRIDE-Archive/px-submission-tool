package uk.ac.ebi.pride.gui.form;

import org.jdesktop.swingx.prompt.PromptSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.blocker.DefaultGUIBlocker;
import uk.ac.ebi.pride.gui.blocker.GUIBlocker;
import uk.ac.ebi.pride.gui.data.FeedbackFormModel;
import uk.ac.ebi.pride.gui.navigation.Navigator;
import uk.ac.ebi.pride.gui.task.TaskAdapter;
import uk.ac.ebi.pride.gui.task.TaskEvent;
import uk.ac.ebi.pride.gui.task.TaskListenerAdapter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;

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
public class FeedbackFormController extends Form implements ActionListener {
    private static final Logger logger = LoggerFactory.getLogger(SubmissionForm.class);

    // Button action commands
    private static final String RATING_BUTTON_ACTION_VERY_BAD = "rating_button_action_very_bad";
    private static final String RATING_BUTTON_ACTION_BAD = "rating_button_action_bad";
    private static final String RATING_BUTTON_ACTION_NEUTRAL = "rating_button_action_neutral";
    private static final String RATING_BUTTON_ACTION_GOOD = "rating_button_action_good";
    private static final String RATING_BUTTON_ACTION_VERY_GOOD = "rating_button_action_very_good";

    // Submission data
    private FeedbackFormModel model;

    // Form components for user feedback collection
    private JPanel feedbackFramePanel;
    private JPanel feedbackMainPanel;
    private JPanel ratingPanel;
    private JPanel additionalFeedbackInfoPanel;
    private JTextArea feedbackAdditionalInfoText;
    private ButtonGroup ratingButtonsGroup;
    // Components to tell user that feedback is being submitted
    private JPanel feedbackSubmissionPanel;
    private JLabel waitingIcon;
    private JLabel submissionWaitingMessage;
    // Successful submission of feedback
    private JPanel feedbackSubmissionSuccessPanel;
    private JLabel feedbackSubmissionSuccessMessage;
    private JLabel feedbackSubmissionSuccessIcon;
    // Failed feedback submission
    private JPanel feedbackSubmissionFailPanel;
    private JLabel feedbackSubmissionFailMessage;
    private JLabel feedbackSubmissionFailIcon;

    // Toggle between feedback panels
    private boolean fpanelToggle = true;

    // Prent form
    private Form parentForm = null;
    private Object constraints = null;

    public FeedbackFormController(String subRef) {
        model = new FeedbackFormModel(subRef);
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
        JRadioButton veryBadExperienceButton = new JRadioButton(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.very_bad.icon.not_selected")));
        veryBadExperienceButton.setSelectedIcon(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.very_bad.icon.selected")));
        veryBadExperienceButton.setRolloverIcon(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.very_bad.icon.not_selected.rollover")));
        veryBadExperienceButton.setRolloverEnabled(true);
        veryBadExperienceButton.setOpaque(true);
        veryBadExperienceButton.setToolTipText(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.very_bad.tooltip"));
        veryBadExperienceButton.setActionCommand(RATING_BUTTON_ACTION_VERY_BAD);
        veryBadExperienceButton.addActionListener(this);
        // Bad experience
        JRadioButton badExperienceButton = new JRadioButton(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.bad.icon.not_selected")));
        badExperienceButton.setSelectedIcon(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.bad.icon.selected")));
        badExperienceButton.setRolloverIcon(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.bad.icon.not_selected.rollover")));
        badExperienceButton.setRolloverEnabled(true);
        badExperienceButton.setToolTipText(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.bad.tooltip"));
        badExperienceButton.setOpaque(false);
        badExperienceButton.setActionCommand(RATING_BUTTON_ACTION_BAD);
        badExperienceButton.addActionListener(this);
        // Neutral experience
        JRadioButton neutralExperienceButton = new JRadioButton(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.neutral.icon.not_selected")));
        neutralExperienceButton.setSelectedIcon(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.neutral.icon.selected")));
        neutralExperienceButton.setRolloverIcon(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.neutral.icon.not_selected.rollover")));
        neutralExperienceButton.setRolloverEnabled(true);
        neutralExperienceButton.setToolTipText(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.neutral.tooltip"));
        neutralExperienceButton.setOpaque(false);
        neutralExperienceButton.setSelected(true);
        neutralExperienceButton.setActionCommand(RATING_BUTTON_ACTION_NEUTRAL);
        neutralExperienceButton.addActionListener(this);
        // Good experience
        JRadioButton goodExperienceButton = new JRadioButton(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.good.icon.not_selected")));
        goodExperienceButton.setSelectedIcon(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.good.icon.selected")));
        goodExperienceButton.setRolloverIcon(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.good.icon.not_selected.rollover")));
        goodExperienceButton.setRolloverEnabled(true);
        goodExperienceButton.setToolTipText(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.good.tooltip"));
        goodExperienceButton.setOpaque(false);
        goodExperienceButton.setActionCommand(RATING_BUTTON_ACTION_GOOD);
        goodExperienceButton.addActionListener(this);
        // Very good experience
        JRadioButton veryGoodExperienceButton = new JRadioButton(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.very_good.icon.not_selected")));
        veryGoodExperienceButton.setSelectedIcon(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.very_good.icon.selected")));
        veryGoodExperienceButton.setRolloverIcon(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.very_good.icon.not_selected.rollover")));
        veryGoodExperienceButton.setRolloverEnabled(true);
        veryGoodExperienceButton.setToolTipText(App.getInstance().getDesktopContext().getProperty("feedback.form.rate.very_good.tooltip"));
        veryGoodExperienceButton.setOpaque(false);
        veryGoodExperienceButton.setActionCommand(RATING_BUTTON_ACTION_VERY_GOOD);
        veryGoodExperienceButton.addActionListener(this);
        // Add buttons to the panel
        ratingPanel.add(veryBadExperienceButton);
        ratingPanel.add(badExperienceButton);
        ratingPanel.add(neutralExperienceButton);
        ratingPanel.add(goodExperienceButton);
        ratingPanel.add(veryGoodExperienceButton);
        // Radio buttons group
        ratingButtonsGroup = new ButtonGroup();
        ratingButtonsGroup.add(veryBadExperienceButton);
        ratingButtonsGroup.add(badExperienceButton);
        ratingButtonsGroup.add(neutralExperienceButton);
        ratingButtonsGroup.add(goodExperienceButton);
        ratingButtonsGroup.add(veryGoodExperienceButton);
        ratingButtonsGroup.clearSelection();
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
        feedbackAdditionalInfoText = new JTextArea(7, 1);
        feedbackAdditionalInfoText.setLineWrap(true);
        feedbackAdditionalInfoText.setWrapStyleWord(true);
        PromptSupport.setPrompt(App.getInstance().getDesktopContext().getProperty("feedback.form.comments.placeholder_text"), feedbackAdditionalInfoText);
        PromptSupport.setFocusBehavior(PromptSupport.FocusBehavior.HIDE_PROMPT, feedbackAdditionalInfoText);
        PromptSupport.setFontStyle(Font.ITALIC, feedbackAdditionalInfoText);
        PromptSupport.setForeground(Color.LIGHT_GRAY, feedbackAdditionalInfoText);
        JScrollPane fbAdditionalInfoScrollPane = new JScrollPane(feedbackAdditionalInfoText);
        //fbAdditionalInfoScrollPane.setAutoscrolls(true);
        // Attach elements to panel
        additionalFeedbackInfoPanel.add(fbAdditionalInfoScrollPane, BorderLayout.CENTER);
        // Add additional feedback panel to main feedback panel
        feedbackMainPanel.add(additionalFeedbackInfoPanel, BorderLayout.SOUTH);
        //feedbackMainPanel.setVisible(true);

        // Submission panel
        feedbackSubmissionPanel = new JPanel(new FlowLayout());
        // Waiting icon
        waitingIcon = new JLabel(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.send.panel.icon")));
        submissionWaitingMessage = new JLabel(App.getInstance().getDesktopContext().getProperty("feedback.send.panel.message.text"));
        submissionWaitingMessage.setFont(submissionWaitingMessage.getFont().deriveFont(Float.parseFloat(App.getInstance().getDesktopContext().getProperty("feedback.send.panel.message.text.font.size"))));
        // Add components to the panel
        feedbackSubmissionPanel.add(waitingIcon);
        feedbackSubmissionPanel.add(submissionWaitingMessage);
        // Set it to not visible
        //feedbackSubmissionPanel.setVisible(false);

        // Successful submission panel
        feedbackSubmissionSuccessPanel = new JPanel(new FlowLayout());
        feedbackSubmissionSuccessPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        feedbackSubmissionSuccessIcon = new JLabel(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.send.success.icon")));
        feedbackSubmissionSuccessMessage = new JLabel(App.getInstance().getDesktopContext().getProperty("feedback.send.success.message.text"));
        feedbackSubmissionSuccessMessage.setFont(feedbackSubmissionSuccessMessage.getFont().deriveFont(Float.parseFloat(App.getInstance().getDesktopContext().getProperty("feedback.send.success.message.text.font.size"))));
        feedbackSubmissionSuccessPanel.add(feedbackSubmissionSuccessMessage);
        feedbackSubmissionSuccessPanel.add(feedbackSubmissionSuccessIcon);
        // Failed submission panel
        feedbackSubmissionFailPanel = new JPanel(new FlowLayout());
        feedbackSubmissionFailPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        feedbackSubmissionFailIcon = new JLabel(GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.send.fail.icon")));
        feedbackSubmissionFailMessage = new JLabel(App.getInstance().getDesktopContext().getProperty("feedback.send.fail.message.text"));
        feedbackSubmissionFailMessage.setFont(feedbackSubmissionFailMessage.getFont().deriveFont(Float.parseFloat(App.getInstance().getDesktopContext().getProperty("feedback.send.fail.message.text.font.size"))));
        feedbackSubmissionFailPanel.add(feedbackSubmissionFailMessage);
        feedbackSubmissionFailPanel.add(feedbackSubmissionFailIcon);

        // Add main panel to frame panel
        feedbackFramePanel.add(feedbackMainPanel, BorderLayout.CENTER);
        // Add submission panel
        //feedbackFramePanel.add(feedbackSubmissionPanel, BorderLayout.CENTER);
    }

    public void addToParentForm(Form form, Object constraints) {
        logger.debug("Adding Feedback form to parent form");
        parentForm = form;
        this.constraints = constraints;
        form.add(feedbackFramePanel, constraints);
        form.revalidate();
        form.repaint();
        // Prepare the buttons
        Navigator navigator = ((App) App.getInstance()).getNavigator();
        JButton nextButton = navigator.getNextButton();

        nextButton.setText(App.getInstance().getDesktopContext().getProperty("feedback.button.submit.label"));
    }

    private void showUserFeedbackSubmissionMessage() {
        logger.debug("Showing wait message for user feedback submission");
        Navigator navigator = ((App) App.getInstance()).getNavigator();
        JButton nextButton = navigator.getNextButton();
        Icon newIcon = GUIUtilities.loadIcon(appContext.getProperty("feedback.button.submit.submitting_state.icon"));
        nextButton.setIcon(newIcon);
        feedbackFramePanel.removeAll();
        feedbackFramePanel.add(feedbackSubmissionPanel, BorderLayout.CENTER);
        feedbackFramePanel.revalidate();
        feedbackFramePanel.repaint();
    }

    private void showSuccessFeedbackSubmission() {
        logger.debug("Successful user feedback submission");
        feedbackFramePanel.removeAll();
        feedbackFramePanel.add(feedbackSubmissionSuccessPanel, BorderLayout.CENTER);
        feedbackFramePanel.revalidate();
        feedbackFramePanel.repaint();

    }

    private void showErrorFeedbackSubmission() {
        logger.error("Error occurred while submitting user feedback");
        feedbackFramePanel.removeAll();
        feedbackFramePanel.add(feedbackSubmissionFailPanel, BorderLayout.CENTER);
        feedbackFramePanel.revalidate();
        feedbackFramePanel.repaint();

    }

    public boolean doSubmitFeedbackOnClose() {
        //return (model.isFeedbackSubmitted() || doSubmitFeedback());
        // Is feedback mandatory?
        if (FeedbackSubmissionHelper.isFeedbackMandatory()) {
            // I'm pretty sure this could be beautify in the future from the OOP point of view but, right now, this is
            // the smallest change that accomplishes this goal given the current requirements
            if (!model.isFeedbackSubmitted()) {
                JOptionPane.showMessageDialog((Component) null, App.getInstance().getDesktopContext().getProperty("feedback.form.confirmation_dialog.message"),
                        "Feedback", JOptionPane.OK_OPTION, GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.confirmation_dialog.icon")));
                return false;
            }
        }
        return true;
    }

    public boolean doSubmitFeedback(TaskListenerAdapter descriptorListener) {
        if (model.isFeedbackSubmitted())
            return true;
        if (FeedbackSubmissionHelper.isFeedbackMandatory()) {
            // I'm pretty sure this could be beautify in the future from the OOP point of view but, right now, this is
            // the smallest change that accomplishes this goal given the current requirements
            if (!model.isFeedbackProvided()) {
                JOptionPane.showMessageDialog((Component) null, App.getInstance().getDesktopContext().getProperty("feedback.form.confirmation_dialog.message"),
                        "Feedback", JOptionPane.OK_OPTION, GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("feedback.form.confirmation_dialog.icon")));
                return false;
            }
        }
        model.setComment(feedbackAdditionalInfoText.getText());
        FeedbackSubmissionTask task = new FeedbackSubmissionTask(this);
        FeedbackSubmissionTaskListener listener = new FeedbackSubmissionTaskListener(this);
        task.addTaskListener(listener);
        task.addTaskListener(descriptorListener);
        task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));
        App.getInstance().getDesktopContext().addTask(task);
        return false;
    }

    private void applyRadioSelectionStyle(AbstractButton b) {
        //b.setRolloverEnabled(false);
        //b.setBackground(Color.decode("#FF2400"));
        Navigator navigator = ((App) App.getInstance()).getNavigator();
        JButton nextButton = navigator.getNextButton();
        nextButton.setEnabled(true);
    }

    private void applyRadioDeselectedStyle(AbstractButton b) {
        //b.setRolloverEnabled(true);
        //b.setBackground(null);
    }

    private void updateRadioButtonsAspect() {
        for (AbstractButton b : Collections.list(ratingButtonsGroup.getElements())) {
            if (b.isSelected()) {
                applyRadioSelectionStyle(b);
            } else {
                applyRadioDeselectedStyle(b);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(RATING_BUTTON_ACTION_VERY_BAD)) {
            model.setRating(FeedbackFormModel.RATING_VERY_BAD);
        } else if (e.getActionCommand().equals(RATING_BUTTON_ACTION_BAD)) {
            model.setRating(FeedbackFormModel.RATING_BAD);
        } else if (e.getActionCommand().equals(RATING_BUTTON_ACTION_NEUTRAL)) {
            model.setRating(FeedbackFormModel.RATING_NEUTRAL);
        } else if (e.getActionCommand().equals(RATING_BUTTON_ACTION_GOOD)) {
            model.setRating(FeedbackFormModel.RATING_GOOD);
        } else if (e.getActionCommand().equals(RATING_BUTTON_ACTION_VERY_GOOD)) {
            model.setRating(FeedbackFormModel.RATING_VERY_GOOD);
        } else {
            logger.error("Action Command '" + e.getActionCommand() + "' NOT RECOGNIZED");
        }
        updateRadioButtonsAspect();
    }

    // Feedback submission task
    private class FeedbackSubmissionTask extends TaskAdapter<Boolean, Void> {
        // Feedback model
        private FeedbackFormController formController;

        public FeedbackSubmissionTask(FeedbackFormController controller) {
            formController = controller;
        }

        public FeedbackFormController getController() {
            return formController;
        }

        @Override
        protected Boolean doInBackground() throws Exception {
            formController.showUserFeedbackSubmissionMessage();
            return model.save();
        }
    }

    // Feedback submission task listener
    private class FeedbackSubmissionTaskListener extends TaskListenerAdapter<Boolean, Void> {
        // Form controller
        private FeedbackFormController formController;

        public FeedbackSubmissionTaskListener(FeedbackFormController formController) {
            this.formController = formController;
        }

        @Override
        public void failed(TaskEvent<Throwable> event) {
            // Show dialog with alternative link
            formController.showErrorFeedbackSubmission();
        }

        @Override
        public void succeed(TaskEvent<Boolean> booleanTaskEvent) {
            // Show feedback completion
            formController.showSuccessFeedbackSubmission();
        }
    }
}
