package uk.ac.ebi.pride.gui.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data
 * Timestamp: 2016-04-27 13:55
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
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

    // Feedback information
    private String submissionRef;
    private int rating = -1;
    private String comment = "";

    public FeedbackFormModel(String submissionRef) {
        this.submissionRef = submissionRef;
        logger.debug("FeedbackForm model created for submission reference " + submissionRef);
    }

    public boolean isFeedbackProvided() {
        return feedbackProvided;
    }

    public boolean isFeedbackProvisionRejected() {
        return feedbackProvisionRejected;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public void setFeedbackProvided(boolean feedbackProvided) {
        this.feedbackProvided = feedbackProvided;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setFeedbackProvisionRejected(boolean feedbackProvisionRejected) {
        this.feedbackProvisionRejected = feedbackProvisionRejected;
    }
}
