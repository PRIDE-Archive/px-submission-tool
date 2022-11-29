package uk.ac.ebi.pride.gui.form;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 *  Resubmission Summary Descriptor
 *
 */
public class ResubmissionSummaryDescriptor extends SummaryDescriptor {

    private static final Logger logger = LoggerFactory.getLogger(ResubmissionSummaryDescriptor.class);

    public ResubmissionSummaryDescriptor(String id, String title, String desc, Component form) {
        super(id, title, desc, form);
    }

    @Override
    public boolean toSkipPanel() {
        final String resubmissionPxAccession = appContext.getSubmissionRecord().getSubmission().getProjectMetaData().getResubmissionPxAccession();
        return resubmissionPxAccession == null;
    }
}

