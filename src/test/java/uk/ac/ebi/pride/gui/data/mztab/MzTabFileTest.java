package uk.ac.ebi.pride.gui.data.mztab;

import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab
 * Timestamp: 2016-06-07 15:55
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class MzTabFileTest {
    private static final Logger logger = LoggerFactory.getLogger(MzTabFileTest.class);
    private List<String> validFiles;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    public MzTabFileTest() throws URISyntaxException {
        //validFiles = Arrays.asList("src/test/resources/sample_data/valid1.mzTab.gz", "src/test/resources/sample_data/valid2.mzTab");
        validFiles = Arrays.asList(getClass().getClassLoader().getResource("sample_data/valid1.mzTab.gz").getPath(),
                getClass().getClassLoader().getResource("sample_data/valid2.mzTab").getPath());
        logger.debug("Files: " + validFiles.toString());
    }

    @Test
    public void checksIfFileExists() throws FileNotFoundException {
        for (String currentFileName :
                validFiles) {
            MzTabFile mzTabFile = new MzTabFile(currentFileName);
            assertEquals("File names DO NOT MATCH", currentFileName, mzTabFile.getFileName());
        }
    }

    @Test
    public void raisesErrorIfFileDoesntExist() throws FileNotFoundException {
        exception.expect(FileNotFoundException.class);
        MzTabFile mzTabFile = new MzTabFile("imnothere.mzTab");
    }
}