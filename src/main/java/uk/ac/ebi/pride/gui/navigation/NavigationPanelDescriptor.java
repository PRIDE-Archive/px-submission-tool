package uk.ac.ebi.pride.gui.navigation;

import uk.ac.ebi.pride.toolsuite.gui.prop.PropertyChangeHelper;

import java.awt.*;

/**
 * NavigationPanelDescriptor contains the logic for displaying and hiding a navigation panel
 *
 * @author Rui Wang
 * @version $Id$
 */
public class NavigationPanelDescriptor extends PropertyChangeHelper {

    /**
     * Must notify using this property after calling beforeDisplayingPanel method
     */
    public static final String BEFORE_DISPLAY_PANEL_PROPERTY = "beforeDisplayPanelProperty";
    /**
     * Must notify using this property after calling displayingPanel method
     */
    public static final String DISPLAYING_PANEL_PROPERTY = "displayingPanelProperty";
    /**
     * Must notify using this property after calling beforeHidingForNextPanel method
     */
    public static final String BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY = "beforeHidingForNextPanelProperty";
    /**
     * Must notify using this property after calling beforeHidingForPreviousPanel method
     */
    public static final String BEFORE_HIDING_FOR_PREVIOUS_PANEL_PROPERTY = "beforeHidingForPreviousPanelProperty";
    /**
     * Must notify using this property to enable finish button
     */
    public static final String BEFORE_FINISH_PROPERTY = "beforeFinishProperty";

    // User Feedback properties
    /**
     * This property is fired to prepare "feedback submission" button
     */
    public static final String BEFORE_SUBMITTING_FEEDBACK_PROPERTY = "beforeSubmittingFeedbackProperty";

    // Training mode properties
    /**
     * This property is the event that fires when training mode checkbox is touched
     */
    public static final String TRAINING_MODE_TOGGLE_PROPERTY = "training_mode_toggle_property";

    /**
     * Indicate finish of the navigation
     */
    public static final NavigationPanelDescriptor FINISH = new FinishNavigation();

    /**
     * Id to uniquely identify this navigationPanelDescriptor
     */
    private Object navigationPanelId = null;

    /**
     * Navigation panel title
     */
    private String title = null;

    /**
     * Navigation panel description
     */
    private String description = null;
    /**
     * Panel to be shown in a container
     */
    private Component navigationPanel = null;

    public NavigationPanelDescriptor(Object navigationId,
                                     String title,
                                     String description,
                                     Component navigationPanel) {
        this.navigationPanelId = navigationId;
        this.title = title;
        this.description = description;
        this.navigationPanel = navigationPanel;
    }

    /**
     * Get navigation panel id
     *
     * @return Object  navigation panel id
     */
    public final Object getNavigationPanelId() {
        return navigationPanelId;
    }

    /**
     * Get the title of this panel
     *
     * @return String  panel title
     */
    public final String getTitle() {
        return title;
    }

    /**
     * Get the description of this panel
     *
     * @return String  panel description
     */
    public final String getDescription() {
        return description;
    }

    /**
     * Get navigation panel
     *
     * @return Component   navigation panel
     */
    public final Component getNavigationPanel() {
        return navigationPanel;
    }

    /**
     * Method to be performed to show help document
     * Override this method to add help functionality
     */
    public void getHelp() {
    }

    /**
     * Method to check whether to skip this panel
     * Override this method to add additional functionality
     */
    public boolean toSkipPanel() {
        return false;
    }

    /**
     * Method to be performed before the panel is to be displayed.
     * Override this method to add additional functionality
     */
    public void beforeDisplayingPanel() {
        firePropertyChange(BEFORE_DISPLAY_PANEL_PROPERTY, false, true);
    }


    /**
     * Method to be performed when the panel is displayed
     * Override this method to add additional functionality
     */
    public void displayingPanel() {
        firePropertyChange(DISPLAYING_PANEL_PROPERTY, false, true);
    }

    /**
     * Method to be performed when the panel is to be hidden
     * before moving to the next panel
     * Override this method to add additional functionality
     */
    public void beforeHidingForNextPanel() {
        firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, false, true);
    }

    /**
     * Method to be performed when the panel is to be hidden
     * before moving to the previous panel
     * Override this method to add additional functionality
     */
    public void beforeHidingForPreviousPanel() {
        firePropertyChange(BEFORE_HIDING_FOR_PREVIOUS_PANEL_PROPERTY, false, true);
    }


    /**
     * Finish the entire navigation
     */
    public static class FinishNavigation extends NavigationPanelDescriptor {
        public static final String ID = "Finished";


        public FinishNavigation() {
            super(ID, null, null, null);
        }
    }
}
