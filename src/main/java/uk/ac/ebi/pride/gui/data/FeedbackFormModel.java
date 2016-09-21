package uk.ac.ebi.pride.gui.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.gui.form.FeedbackSubmissionHelper;
import uk.ac.ebi.pride.px.CompositeReport;
import uk.ac.ebi.pride.px.ReportFactory;
import uk.ac.ebi.pride.px.SubmissionRecordReport;
import uk.ac.ebi.pride.px.UserFeedbackReport;
import uk.ac.ebi.pride.px.reports.ReportBuilder;
import uk.ac.ebi.pride.px.reports.ReportBuilderException;
import uk.ac.ebi.pride.px.reports.ReportProduct;
import uk.ac.ebi.pride.px.reports.builders.GoogleSpreadsheetBuilder;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data
 * Timestamp: 2016-04-27 13:55
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 * <p>
 * ---
 * This is the model used by the FeedbackFormController, when providing user feedback at the end of the submission
 * process.
 */
public class FeedbackFormModel {
    public static final Logger logger = LoggerFactory.getLogger(FeedbackFormModel.class);

    // Constants
    public static final int RATING_VERY_BAD = 0;
    public static final int RATING_BAD = 1;
    public static final int RATING_NEUTRAL = 2;
    public static final int RATING_GOOD = 3;
    public static final int RATING_VERY_GOOD = 4;

    // State variables
    private boolean feedbackProvided = false;
    private boolean feedbackProvisionRejected = false;
    private boolean feedbackSubmitted = false;

    // Feedback information
    private UserFeedbackReport userFeedbackReport = ReportFactory.getFactory().getUserFeedbackReport();
    private SubmissionRecordReport submissionRecordReport = ReportFactory.getFactory().getSubmissionRecordReport();

    public FeedbackFormModel(String submissionRef) {
        submissionRecordReport.setSubmissionReference(submissionRef);
        logger.debug("FeedbackForm model created for submission reference " + submissionRef);
    }

    public boolean isFeedbackProvided() {
        return feedbackProvided;
    }

    public boolean isFeedbackProvisionRejected() {
        return feedbackProvisionRejected;
    }

    public int getRating() {
        return userFeedbackReport.getRating();
    }

    public String getComment() {
        return userFeedbackReport.getComments();
    }

    public void setFeedbackProvided(boolean feedbackProvided) {
        this.feedbackProvided = feedbackProvided;
    }

    public void setRating(int rating) {
        setFeedbackProvided(true);
        userFeedbackReport.setRating(rating);
        logger.debug("Set user feedback rating to: " + rating);
    }

    public void setComment(String comment) {
        logger.debug("Setting comment to: '" + comment + "'");
        userFeedbackReport.setComments(comment);
    }

    public boolean isFeedbackSubmitted() {
        return feedbackSubmitted;
    }

    public void setFeedbackProvisionRejected(boolean feedbackProvisionRejected) {
        this.feedbackProvisionRejected = feedbackProvisionRejected;

    }

    public boolean save() {
        if (FeedbackSubmissionHelper.isFeedbackMandatory()) {
            if (!isFeedbackProvided()) {
                logger.debug("No feedback has been provided, and the user didn't reject providing feedback.");
                return false;
            }
            if (!isFeedbackSubmitted()) {
                // Do send feedback
                CompositeReport compositeReport = ReportFactory.getFactory().getCompositeReport();
                compositeReport.add(submissionRecordReport);
                compositeReport.add(userFeedbackReport);
                ReportBuilder builder = new GoogleSpreadsheetBuilder(
                        App.getInstance().getDesktopContext().getProperty("libpxreport.keyfile.path"),
                        App.getInstance().getDesktopContext().getProperty("libpxreport.account.id"),
                        App.getInstance().getDesktopContext().getProperty("libpxreport.spreadsheet.title"),
                        App.getInstance().getDesktopContext().getProperty("libpxreport.spreadsheet.worksheet.title"),
                        App.getInstance().getDesktopContext().getProperty("libpxreport.client.service.name"));
                compositeReport.save(builder);
                try {
                    ReportProduct product = builder.getProduct();
                } catch (ReportBuilderException e) {
                    logger.error("Something, happened when submitting the report, but I'm not addressing this right now");
                }
                logger.debug("Submitting feedback from user...");
                logger.info("User feedback for submission reference '" + submissionRecordReport.getSubmissionReference() + "', rating '" + getRating() + "', comments '" + getComment() + "'");
            }
        } else {
            logger.debug("No actual feedback submission will be performed at the model level, as it is not mandatory");
        }
        feedbackSubmitted = true;
        return true;
    }
}
