package uk.ac.ebi.pride.gui.data.mztab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab
 * Timestamp: 2016-06-07 15:54
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This Class models the core information we need form an mzTab file, for data submission purposes
 */

public class MzTabFile {
    private static final Logger logger = LoggerFactory.getLogger(MzTabFile.class);

    // File name / path
    private String fileName;

    public MzTabFile(String fileName) throws FileNotFoundException {
        this.fileName = fileName;
        // Check if the file exists
        new FileInputStream(fileName);
    }

    public String getFileName() {
        return fileName;
    }

}
