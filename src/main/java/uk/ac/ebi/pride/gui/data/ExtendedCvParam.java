package uk.ac.ebi.pride.gui.data;

import uk.ac.ebi.pride.data.model.CvParam;

/**
 * Extended cv parameter with human readable name
 *
 * @author Rui Wang
 * @version $Id$
 */
public class ExtendedCvParam extends CvParam {

    private String humanReadableName;

    public ExtendedCvParam(String cvLabel, String accession, String name, String humanReadableName, String value) {
        super(cvLabel, accession, name, value);
        this.humanReadableName = humanReadableName;
    }

    public String getHumanReadableName() {
        return humanReadableName;
    }

    public void setHumanReadableName(String humanReadableName) {
        this.humanReadableName = humanReadableName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CvParam)) return false;
        if (!super.equals(o)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
