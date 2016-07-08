package uk.ac.ebi.pride.gui.data.mztab.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.exceptions.InvalidCvParameterException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.model
 * Timestamp: 2016-07-06 11:44
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class VariableMod {
    private static final Logger logger = LoggerFactory.getLogger(VariableMod.class);

    public class VariableModValue extends CvParameter {
        public VariableModValue(String label, String accession, String name, String value) throws InvalidCvParameterException {
            super(label, accession, name, value);
        }

        public VariableModValue(CvParameter cv) {
            super(cv);
        }
    }

    // Bean
    private VariableModValue value = null;
    private String site = null;
    private String position = null;

    public VariableMod() {
        // TODO - Any additional steps to be taken here?
    }

    public VariableModValue getValue() {
        return value;
    }

    public void setValue(CvParameter value) {
        this.value = new VariableModValue(value);
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public boolean validate() throws ValidationException {
        // A value must have been provided
        if (getValue() == null) {
            logger.error("MISSING value for variable_mod metadata attribute");
            return false;
        }
        if (!getValue().validate()) {
            logger.error("variable_mod value IS INVALID!!!");
            return false;
        }
        return true;
    }
}
