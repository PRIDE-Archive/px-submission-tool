package uk.ac.ebi.pride.gui.data.mztab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab
 * Timestamp: 2016-06-08 11:17
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This class models a CV Parameter
 */

public class CvParameter {
    private static final Logger logger = LoggerFactory.getLogger(CvParameter.class);

    // Bean
    private String label;
    private String accession;
    private String name;
    private String value;

    public CvParameter() {
        label = "";
        accession = "";
        name = "";
        value = "";
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
