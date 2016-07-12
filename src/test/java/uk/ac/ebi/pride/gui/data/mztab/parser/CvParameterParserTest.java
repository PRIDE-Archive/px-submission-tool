package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Ignore;
import org.junit.Test;
import uk.ac.ebi.pride.gui.data.mztab.model.CvParameter;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.CvParameterParserException;

import static org.junit.Assert.*;
import static org.hamcrest.text.IsEqualIgnoringCase.*;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-20 09:59
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class CvParameterParserTest {

    @Test
    public void nullOnEmptyString() {
        assertNull(CvParameterParser.fromString(""));
    }

    @Test(expected = CvParameterParserException.class)
    public void missingStartingSquareBracket() {
        CvParameterParser.fromString("MS, MS:1002453, No fixed modifications searched, ]");
    }

    @Test(expected = CvParameterParserException.class)
    public void missingTrailingSquareBracket() {
        CvParameterParser.fromString("[MS, MS:1002453, No fixed modifications searched, ");
    }

    @Test
    public void captureMissingValueAsEmpty() {
        CvParameter cvParameter = CvParameterParser.fromString("[MS, MS:1002453, No fixed modifications searched, ]");
        assertTrue("Missing value is captured as empty string/value", cvParameter.getValue().isEmpty());
    }

    @Test
    public void missingNameAsEmpty() {
        CvParameter cvParameter = CvParameterParser.fromString("[MS, MS:1002453, , sample value]");
        assertTrue("Missing name is captured as empty string/value", cvParameter.getName().isEmpty());
    }

    // We are now ignoring this test because, there are cases like '[MS, MS:1001171, Mascot:score,]' where, being the
    // comma so close to the ']' makes the array split into 3 items instead of 4 and, anyway, when there is a space
    // after that comma, we get an empty fourth value, which is legal
    @Test(expected = CvParameterParserException.class)
    @Ignore
    public void errorOnMissingItem() {
        CvParameterParser.fromString("[MS, MS:1002453, No fixed modifications searched ]");
    }

    @Test
    public void checkThatParsedValuesMatchOriginal() {
        String label = "MS";
        String accession = "MS:1002453";
        String name = "No fixed modifications searched";
        String value = "sample value";
        String cvParamInputLine = "[ " + label + ", "
                + accession + ", "
                + name + ", "
                + value + "    ]";
        CvParameter cvParameter = CvParameterParser.fromString(cvParamInputLine);
        assertThat("Labels matches", cvParameter.getLabel(), equalToIgnoringCase(label));
        assertThat("Accessions matches", cvParameter.getAccession(), equalToIgnoringCase(accession));
        assertThat("Name matches", cvParameter.getName(), equalToIgnoringCase(name));
        assertThat("Value matches", cvParameter.getValue(), equalToIgnoringCase(value));
    }
}