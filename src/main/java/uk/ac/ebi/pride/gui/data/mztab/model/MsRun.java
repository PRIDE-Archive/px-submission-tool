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
    private MsRunFormat msRunFormat;
    private MsRunIdFormat msRunIdFormat;
    private URL location;

    public MsRun(MsRunFormat msRunFormat, MsRunIdFormat msRunIdFormat, URL location) {
        this.msRunFormat = msRunFormat;
        this.msRunIdFormat = msRunIdFormat;
        this.location = location;
    }
}
