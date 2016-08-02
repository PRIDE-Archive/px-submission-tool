package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.form
 * Timestamp: 2016-08-02 12:09
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This class helps to things like taking decisions on feedback submission
 */
public class FeedbackSubmissionHelper {
    private static AppContext appContext = (AppContext) App.getInstance().getDesktopContext();

    public static boolean isFeedbackMandatory() {
        return !appContext.isTrainingModeFlag();
    }
}
