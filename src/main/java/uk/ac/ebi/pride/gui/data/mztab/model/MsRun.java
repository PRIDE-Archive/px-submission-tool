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
    private MsRunHashMethod hashMethod = null;
    private String hash = null;

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

    public MsRunHashMethod getHashMethod() {
        return hashMethod;
    }

    public void setHashMethod(MsRunHashMethod hashMethod) {
        this.hashMethod = hashMethod;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public boolean validate() throws ValidationException {
        // Validation Criteria
        // location must always be reported
        if (getLocation() == null) {
            logger.error("MISSING ms_run location information");
            return false;
        }
        // If ms-run format is reported, ms-run id_format is required
        if ((getMsRunFormat() != null) && (getMsRunIdFormat() == null)) {
            logger.error("ms_run format specified, but MISSING ms-run ID_format");
            return false;
        }
        if ((getMsRunFormat() != null) && (!getMsRunFormat().validate())) {
            logger.error("ms_run format IS INVALID");
            return false;
        }
        if ((getMsRunIdFormat() != null) && (!getMsRunIdFormat().validate())) {
            logger.error("ms_run ID format IS INVALID");
            return false;
        }
        if ((getHash() != null) && (getHashMethod() == null)) {
            logger.error("ms_run hash is present BUT ms_run hash_method IS MISSING!");
            return false;
        }
        if ((getHashMethod() != null) && (!getHashMethod().validate())) {
            logger.error("This ms_run DOES NOT VALIDATE because its hash_method IS INVALID");
            return false;
        }
        return true;
    }
}
