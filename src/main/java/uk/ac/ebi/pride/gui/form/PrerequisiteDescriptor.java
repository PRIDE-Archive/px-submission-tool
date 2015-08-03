package uk.ac.ebi.pride.gui.form;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareNavigationPanelDescriptor;

import javax.help.HelpBroker;

/**
 * Navigation descriptor for Prerequsite form, which display a list of required items ahead of submission
 *
 * @author Rui Wang
 * @version $Id$
 */
public class PrerequisiteDescriptor extends ContextAwareNavigationPanelDescriptor {
    private static final Logger logger = LoggerFactory.getLogger(PrerequisiteDescriptor.class);

    public PrerequisiteDescriptor(String id, String title, String desc) {
        super(id, title, desc, new PrerequisiteForm());
    }

    @Override
    public void getHelp() {
        HelpBroker hb = appContext.getMainHelpBroker();
        hb.showID("help.prerequisite", "javax.help.SecondaryWindow", "main");
    }

    @Override
    public void beforeDisplayingPanel() {
        Submission submission = appContext.getSubmissionRecord().getSubmission();
        SubmissionType submissionType = submission.getProjectMetaData().getSubmissionType();

        // set submission type
        PrerequisiteForm form = (PrerequisiteForm) getNavigationPanel();
        form.setSubmissionType(submissionType);

        firePropertyChange(BEFORE_DISPLAY_PANEL_PROPERTY, false, true);
    }


}
