package uk.ac.ebi.pride.gui.navigation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.gui.form.FeedbackSubmissionHelper;
import uk.ac.ebi.pride.gui.form.comp.NonOpaquePanel;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * The main class is responsible for create all the main components and control the navigation
 *
 * @author Rui Wang
 * @version $Id$
 */
public class Navigator extends JPanel implements PropertyChangeListener {
    public static final Logger logger = LoggerFactory.getLogger(Navigator.class);
    /**
     * Action command generated by the control panel
     */
    public static final String CANCEL_BUTTON_ACTION = "Cancel";
    public static final String NEXT_BUTTON_ACTION = "Next";
    public static final String BACK_BUTTON_ACTION = "Back";
    public static final String HELP_BUTTON_ACTION = "Help";

    public static final int DEFAULT_BUTTON_WIDTH = 90;
    public static final int DEFAULT_BUTTON_HEIGHT = 30;


    private NavigationModel navigationModel;
    private NavigationController navigationController;

    /**
     * Navigation card panel
     */
    private JPanel cardPanel;
    private CardLayout cardLayout;

    private JLabel titleLabel;
    private JLabel descLabel;

    private JButton cancelButton;
    private JButton backButton;
    private JButton nextButton;

    /**
     * Tittle panel
     */
    private NavigationTitlePanel titlePanel = null;

    /**
     * Id of the panel about to become current panel
     */
    private Object stagingPanelId;


    public Navigator() {

        initComponents();
    }

    private void initComponents() {
        navigationModel = new NavigationModel();
        navigationController = new NavigationController(this);
        navigationModel.addPropertyChangeListener(this);

        // setup main pane
        this.setLayout(new BorderLayout());

        // title panel
        initTitlePanel();

        // card panel
        initCardPanel();

        // control panel
        initControlPanel();
    }

    /**
     * Initialize title panel
     */
    private void initTitlePanel() {
        titlePanel = new NavigationTitlePanel();
        // layout
        titlePanel.setLayout(new BorderLayout());

        JPanel layoutPanel = new NonOpaquePanel(new FlowLayout(FlowLayout.LEFT));

        // add gap
        layoutPanel.add(Box.createRigidArea(new Dimension(20, 20)));

        // container panel
        JPanel containerPanel = new NonOpaquePanel();
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));

        // add gap
        containerPanel.add(Box.createRigidArea(new Dimension(10, 10)));

        // title label
        titleLabel = new JLabel();
        titleLabel.setForeground(Color.white);
        titleLabel.setFont(titleLabel.getFont().deriveFont(20f).deriveFont(Font.BOLD));
        containerPanel.add(titleLabel);

        // gap
        containerPanel.add(Box.createRigidArea(new Dimension(20, 10)));

        // description label
        descLabel = new JLabel();
        descLabel.setForeground(Color.white);
        containerPanel.add(descLabel);

        layoutPanel.add(containerPanel);
        titlePanel.add(layoutPanel, BorderLayout.WEST);

        this.add(titlePanel, BorderLayout.NORTH);
    }

    /**
     * Initialize card panel
     */
    private void initCardPanel() {
        // card panel
        cardPanel = new JPanel();
        cardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // layout
        cardLayout = new CardLayout();
        cardPanel.setLayout(cardLayout);

        this.add(cardPanel, BorderLayout.CENTER);
    }

    /**
     *
     */
    private void initControlPanel() {
        // setup main pane
        JPanel controlPanel = new NavigationControlPanel();
        controlPanel.setLayout(new BorderLayout());

        // app context
        DesktopContext appContext = App.getInstance().getDesktopContext();

        // help button
        JPanel helpButtonPanel = new NonOpaquePanel(new FlowLayout(FlowLayout.LEFT));
        JButton helpButton = GUIUtilities.createLabelLikeButton(GUIUtilities.loadIcon(appContext.getProperty("help.button.small.icon")), null);
        helpButton.setPreferredSize(new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        helpButton.setFocusable(false);
        helpButton.setActionCommand(HELP_BUTTON_ACTION);
        helpButton.addActionListener(navigationController);
        helpButtonPanel.add(helpButton);
        controlPanel.add(helpButtonPanel, BorderLayout.WEST);

        // control pane
        JPanel ctrlPane = new NonOpaquePanel(new FlowLayout(FlowLayout.RIGHT));

        // cancel button
        cancelButton = new JButton(appContext.getProperty("cancel.button.label"),
                GUIUtilities.loadIcon(appContext.getProperty("cancel.button.small.icon")));
        cancelButton.setPreferredSize(new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        cancelButton.setActionCommand(CANCEL_BUTTON_ACTION);
        cancelButton.addActionListener(navigationController);
        ctrlPane.add(cancelButton);

        // add gap
        ctrlPane.add(Box.createRigidArea(new Dimension(40, 10)));

        // back button
        backButton = new JButton(appContext.getProperty("back.button.label"),
                GUIUtilities.loadIcon(appContext.getProperty("back.button.small.icon")));
        backButton.setPreferredSize(new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        backButton.setActionCommand(BACK_BUTTON_ACTION);
        backButton.addActionListener(navigationController);
        ctrlPane.add(backButton);

        // next button
        nextButton = new JButton(appContext.getProperty("next.button.label"),
                GUIUtilities.loadIcon(appContext.getProperty("next.button.small.icon")));
        nextButton.setHorizontalTextPosition(SwingConstants.LEADING);
        nextButton.setPreferredSize(new Dimension(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT));
        nextButton.setActionCommand(NEXT_BUTTON_ACTION);
        nextButton.addActionListener(navigationController);
        ctrlPane.add(nextButton);

        // add gap
        ctrlPane.add(Box.createRigidArea(new Dimension(10, 10)));

        controlPanel.add(ctrlPane, BorderLayout.EAST);

        this.add(controlPanel, BorderLayout.SOUTH);
    }

    /**
     * Get navigation model
     *
     * @return NavigationModel navigation model
     */
    public NavigationModel getNavigationModel() {
        return navigationModel;
    }

    /**
     * Get navigation controller
     *
     * @return navigation controller
     */
    public NavigationController getNavigationController() {
        return navigationController;
    }

    /**
     * Register navigation panel
     *
     * @param descriptor navigation panel
     * @throws NavigationException error while registering navigation panel
     */
    public void registerNavigationPanel(NavigationPanelDescriptor descriptor) throws NavigationException {
        // add panel to card panel
        if (!descriptor.getNavigationPanelId().equals(NavigationPanelDescriptor.FINISH.getNavigationPanelId())) {
            cardPanel.add(descriptor.getNavigationPanel(), descriptor.getNavigationPanelId());
        }

        // register property listener
        descriptor.addPropertyChangeListener(this);

        // register to model
        navigationModel.registerNavigationPanel(descriptor);
    }

    /**
     * Set a new current navigation panel
     *
     * @param id navigation panel id
     */
    public void  setCurrentNavigationPanel(Object id) {
        // store id for the new current panel
        stagingPanelId = id;

        if (!navigationModel.hasCurrentNavigationPanel()) {
            NavigationPanelDescriptor newPanel = navigationModel.getPanelDescriptor(id);
            newPanel.beforeDisplayingPanel();
        } else if (navigationModel.isNextPanelDescriptor(id)) {
            // call old current panel's
            NavigationPanelDescriptor oldPanel = navigationModel.getCurrentPanelDescriptor();
            oldPanel.beforeHidingForNextPanel();
        } else if (navigationModel.isBackPanelDescriptor(id)) {
            // call old current panel's
            NavigationPanelDescriptor oldPanel = navigationModel.getCurrentPanelDescriptor();
            oldPanel.beforeHidingForPreviousPanel();
        }
    }

    public void close() {
        AppContext context = (AppContext) App.getInstance().getDesktopContext();
        int n = JOptionPane.showConfirmDialog(((App) App.getInstance()).getMainFrame(),
                context.getProperty("cancel.submission.message"),
                context.getProperty("cancel.submission.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (n == 0) {
            App.getInstance().shutdown(null);
        }
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getBackButton() {
        return backButton;
    }

    public JButton getNextButton() {
        return nextButton;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();
        logger.debug("Property change event: " + propName);

        if (NavigationModel.CURRENT_NAVIGATION_PANEL_CHANGE_PROPERTY.equals(propName)) {
            changeCurrentNavigationPanel();
        } else if (NavigationPanelDescriptor.BEFORE_DISPLAY_PANEL_PROPERTY.equals(propName)) {
            handleBeforeDisplayPanelResult(evt);
        } else if (NavigationPanelDescriptor.DISPLAYING_PANEL_PROPERTY.equals(propName)) {
            handleDisplayingPanelResult(evt);
        } else if (NavigationPanelDescriptor.BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY.equals(propName)) {
            handleBeforeHidingForNextPanelResult(evt);
        } else if (NavigationPanelDescriptor.BEFORE_HIDING_FOR_PREVIOUS_PANEL_PROPERTY.equals(propName)) {
            handleBeforeHidingForPreviousPanelResult(evt);
        } else if (NavigationPanelDescriptor.BEFORE_SUBMITTING_FEEDBACK_PROPERTY.equals(propName)) {
            handleBeforeSubmittingFeedback(evt);
        } else if (NavigationPanelDescriptor.TRAINING_MODE_TOGGLE_PROPERTY.equals(propName)) {
            handleTrainingModeToggle(evt);
        } else if (NavigationPanelDescriptor.BEFORE_FINISH_PROPERTY.equals(propName)) {
            handleFinishResult(evt);
        }
    }

    private void handleTrainingModeToggle(PropertyChangeEvent evt) {
        logger.info("Handling Training mode event");
        // TODO - This should be refactored in the future, when property changes in the training checkbox will trigger
        // TODO - an action on a Mediator object that will interact with different things, e.g. disabling file uploads,
        // TODO - making feedback not mandatory, updating the logo of the main window, etc. That's the proper way to do
        // TODO - it, but it all depends on whether this tool survives long enough to make that refactoring worth it or
        // TODO - not.
        titlePanel.updateLogo();
    }

    /**
     * Change the visibility of the buttons as the result of a new front navigation panel
     */
    private void changeCurrentNavigationPanel() {
        logger.debug("About to change current navigation panel");

        // get navigation panels
        NavigationPanelDescriptor nextPanel = navigationModel.getNextPanelDescriptor();
        NavigationPanelDescriptor backPanel = navigationModel.getBackPanelDescriptor();

        if (NavigationPanelDescriptor.FinishNavigation.ID.equals(nextPanel.getNavigationPanelId())) {
            logger.debug("Reached the last navigation panel");
            cancelButton.setVisible(true);
            backButton.setEnabled(true);
            nextButton.setEnabled(false);
            nextButton.setText(App.getInstance().getDesktopContext().getProperty("finish.button.label"));
        } else {
            cancelButton.setEnabled(true);

            backButton.setEnabled(backPanel != null);

            nextButton.setEnabled(true);
            nextButton.setText(App.getInstance().getDesktopContext().getProperty("next.button.label"));
        }

        // update the title panel
        NavigationPanelDescriptor currPanel = navigationModel.getCurrentPanelDescriptor();
        titleLabel.setText(currPanel.getTitle());
        descLabel.setText(currPanel.getDescription());
        // I need to update the logo here, as I miss the first event
        titlePanel.updateLogo();
    }

    /**
     * Handle the result of before hiding panel
     *
     * @param evt property change event
     */
    private void handleBeforeHidingForNextPanelResult(PropertyChangeEvent evt) {
        logger.debug("Handling before hiding panel result");

        if ((Boolean) evt.getNewValue()) {
            // succeed
            NavigationPanelDescriptor nextPanel = navigationModel.getPanelDescriptor(stagingPanelId);
            nextPanel.beforeDisplayingPanel();
        }
    }

    /**
     * Handle the result of before displaying panel
     *
     * @param evt property change event
     */
    private void handleBeforeDisplayPanelResult(PropertyChangeEvent evt) {
        logger.debug("Handling before displaying panel result");

        if ((Boolean) evt.getNewValue()) {
            // succeed
            NavigationPanelDescriptor currPanel = navigationModel.getPanelDescriptor(stagingPanelId);

            // update model
            try {
                navigationModel.setCurrentNavigationPanel(stagingPanelId);
                cardLayout.show(cardPanel, currPanel.getNavigationPanelId().toString());
                this.revalidate();
                this.repaint();

                currPanel.displayingPanel();
            } catch (NavigationException e) {
                logger.error("Failed to updated current navigation panel, Id: " + stagingPanelId.toString());
                // todo: msg box to alert user
            }
        }
    }

    /**
     * Handle the result of hiding before previous panel
     *
     * @param evt property change event
     */
    private void handleBeforeHidingForPreviousPanelResult(PropertyChangeEvent evt) {
        logger.debug("Handling hiding before moving to the previous panel");
        if ((Boolean) evt.getNewValue()) {
            NavigationPanelDescriptor previousPanel = navigationModel.getPanelDescriptor(stagingPanelId);
            previousPanel.beforeDisplayingPanel();
        }
    }

    /**
     * Handle the result of finish entire workflow
     *
     * @param evt property change event
     */
    private void handleFinishResult(PropertyChangeEvent evt) {
        cancelButton.setVisible(false);
        backButton.setVisible(true);
        backButton.setEnabled(true);
        backButton.setText(App.getInstance().getDesktopContext().getProperty("new.submission.button.label"));
        Icon newIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("new.submission.button.small.icon"));
        backButton.setIcon(newIcon);
        nextButton.setVisible(true);
        nextButton.setEnabled(true);
        nextButton.setText(App.getInstance().getDesktopContext().getProperty("finish.button.label"));
        Icon finishIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("finish.button.small.icon"));
        nextButton.setIcon(finishIcon);
        nextButton.setHorizontalTextPosition(SwingConstants.RIGHT);
    }

    private void handleBeforeSubmittingFeedback(PropertyChangeEvent evt) {
        cancelButton.setVisible(false);
        backButton.setVisible(true);
        backButton.setEnabled(!FeedbackSubmissionHelper.isFeedbackMandatory());
        backButton.setText(App.getInstance().getDesktopContext().getProperty("new.submission.button.label"));
        Icon newIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("new.submission.button.small.icon"));
        backButton.setIcon(newIcon);
        nextButton.setVisible(true);
        nextButton.setEnabled(!FeedbackSubmissionHelper.isFeedbackMandatory());
        //nextButton.setEnabled(true);
        //nextButton.setText(App.getInstance().getDesktopContext().getProperty("feedback.button.submit.label"));
        //Icon finishIcon = GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("finish.button.small.icon"));
        //nextButton.setIcon(finishIcon);
        nextButton.setHorizontalTextPosition(SwingConstants.RIGHT);
    }


    /**
     * Handle the result of displaying panel
     *
     * @param evt property change event
     */
    private void handleDisplayingPanelResult(PropertyChangeEvent evt) {
        logger.debug("Handling displaying panel result");
    }
}
