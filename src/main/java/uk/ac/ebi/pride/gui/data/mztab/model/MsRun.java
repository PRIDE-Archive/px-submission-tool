package uk.ac.ebi.pride.gui.data.mztab.model;

import com.itextpdf.text.log.Logger;
import com.itextpdf.text.log.LoggerFactory;

import java.net.URL;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab
 * Timestamp: 2016-06-08 11:02
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This class models an ms-run entry in the metadata section of mzTab files
 */

public class MsRun {
    private static final Logger logger = LoggerFactory.getLogger(MsRun.class);

    // Bean
    private MsRunFormat msRunFormat = null;
    private MsRunIdFormat msRunIdFormat = null;
    private URL location = null;

    public MsRun() {
    }

    public MsRun(MsRunFormat msRunFormat, MsRunIdFormat msRunIdFormat, URL location) {
        this.msRunFormat = msRunFormat;
        this.msRunIdFormat = msRunIdFormat;
        this.location = location;
    }

    public MsRunFormat getMsRunFormat() {
        return msRunFormat;
    }

    public MsRunIdFormat getMsRunIdFormat() {
        return msRunIdFormat;
    }

    public URL getLocation() {
        return location;
    }

    public void setMsRunFormat(MsRunFormat msRunFormat) {
        this.msRunFormat = msRunFormat;
    }

    public void setMsRunIdFormat(MsRunIdFormat msRunIdFormat) {
        this.msRunIdFormat = msRunIdFormat;
    }

    public void setLocation(URL location) {
        this.location = location;
    }

    public boolean validate() throws ValidationException {
        // TODO - Validation Criteria
        return false;
    }
}
