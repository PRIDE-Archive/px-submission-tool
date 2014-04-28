package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.gui.form.comp.ContextAwarePanel;
import uk.ac.ebi.pride.gui.util.ValidationState;

/**
 * The base class to be extended as a form
 *
 * @author Rui Wang
 * @version $Id$
 */
public abstract class Form extends ContextAwarePanel {
    /**
     * Override this method to provide validation business logic
     *
     * @return boolean true means the validation has been successful
     */
    public ValidationState doValidation() {
        return ValidationState.SUCCESS;
    }
}
