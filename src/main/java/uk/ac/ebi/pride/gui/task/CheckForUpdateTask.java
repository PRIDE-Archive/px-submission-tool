package uk.ac.ebi.pride.gui.task;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.gui.util.UpdateChecker;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class CheckForUpdateTask extends TaskAdapter<Boolean, Void> {

    public CheckForUpdateTask() {
        String msg = "Checking for update";
        this.setName(msg);
        this.setDescription(msg);
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        DesktopContext context = App.getInstance().getDesktopContext();
        String updateUrl = context.getProperty("px.submission.tool.update.url");
        String currentVersion = context.getProperty("px.submission.tool.version");

        UpdateChecker updateChecker = new UpdateChecker(updateUrl);

        return updateChecker.hasUpdate(currentVersion);
    }
}

