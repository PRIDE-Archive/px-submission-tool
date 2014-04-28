package uk.ac.ebi.pride.gui.navigation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * NavigationController is responsible for handling the action when the controller buttons are pushed.
 * <p/>
 * controller buttons include next, back and cancel
 *
 * @author Rui Wang
 * @version $Id$
 */
public class NavigationController implements ActionListener {


    private Navigator navigator;


    public NavigationController(Navigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (Navigator.NEXT_BUTTON_ACTION.equals(cmd)) {
            nextButtonPressed();
        } else if (Navigator.BACK_BUTTON_ACTION.equals(cmd)) {
            backButtonPressed();
        } else if (Navigator.CANCEL_BUTTON_ACTION.equals(cmd)) {
            cancelButtonPressed();
        } else if (Navigator.HELP_BUTTON_ACTION.equals(cmd)) {
            helpButtonPressed();
        }
    }

    /**
     * Called when next button pressed
     */
    private void nextButtonPressed() {
        // get current navigation panel
        NavigationModel model = navigator.getNavigationModel();
        NavigationPanelDescriptor nextPanel = model.getNextPanelDescriptor();

        navigator.setCurrentNavigationPanel(nextPanel.getNavigationPanelId());
    }

    /**
     * Called when back button pressed
     */
    private void backButtonPressed() {
        // get current navigation panel
        NavigationModel model = navigator.getNavigationModel();
        NavigationPanelDescriptor previousPanel = model.getBackPanelDescriptor();

        navigator.setCurrentNavigationPanel(previousPanel.getNavigationPanelId());
    }

    /**
     * Cancel button pressed
     */
    private void cancelButtonPressed() {
        navigator.close();
    }

    /**
     * Help button pressed
     */
    private void helpButtonPressed() {
        // get current navigation panel
        NavigationModel model = navigator.getNavigationModel();
        NavigationPanelDescriptor currPanel = model.getCurrentPanelDescriptor();

        // call for help
        currPanel.getHelp();
    }
}
