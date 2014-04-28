package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareNavigationPanelDescriptor;
import uk.ac.ebi.pride.gui.navigation.Navigator;

import javax.help.HelpBroker;
import javax.swing.*;

/**
 * SummaryDescriptor
 *
 * @author Rui Wang
 * @version $Id$
 */
public class SummaryDescriptor extends ContextAwareNavigationPanelDescriptor {

    public SummaryDescriptor(String id, String title, String desc) {
        super(id, title, desc, new SummaryForm());
    }

    /**
     * Method to be performed to show help document
     * Override this method to add help functionality
     */
    @Override
    public void getHelp() {
        HelpBroker hb = appContext.getMainHelpBroker();
        hb.showID("help.submission.summary", "javax.help.SecondaryWindow", "main");
    }

    @Override
    public void displayingPanel() {
        Navigator navigator = ((App) App.getInstance()).getNavigator();
        JButton nextButton = navigator.getNextButton();

        nextButton.setText(appContext.getProperty("summary.submit.button.title"));
    }
}
