package uk.ac.ebi.pride.gui.form.action;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.form.dialog.AddURLDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Action to add a URL representing a file
 *
 * @author Rui Wang
 * @version $Id$
 */
public class AddURLAction extends AbstractAction {

    public AddURLAction() {
        super(App.getInstance().getDesktopContext().getProperty("add.url.button.title"),
                GUIUtilities.loadIcon(App.getInstance().getDesktopContext().getProperty("add.url.button.small.icon")));
        this.putValue(SHORT_DESCRIPTION, App.getInstance().getDesktopContext().getProperty("add.url.button.tooltip"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AddURLDialog dialog = new AddURLDialog(((App)App.getInstance()).getMainFrame());
        dialog.setVisible(true);
    }
}
