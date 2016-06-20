package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.exceptions.InvalidCvParameterException;
import uk.ac.ebi.pride.gui.data.mztab.model.CvParameter;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.CvParameterParserException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-20 09:41
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * Parsing a CvParameter out of a String object
 */

public class CvParameterParser {

    private static class CoreCvParameter extends CvParameter {
        public CoreCvParameter(String label, String accession, String name, String value) throws InvalidCvParameterException {
            super(label, accession, name, value);
        }

        public CoreCvParameter(CvParameter cv) {
            super(cv);
        }
    }

    public static final CvParameter fromString(String s) throws CvParameterParserException {
        s = s.trim();
        if (!s.startsWith("[")) {
            throw new CvParameterParserException("Missing starting '['");
        }
        if (!s.endsWith("]")) {
            throw new CvParameterParserException("Missing closing ']'");
        }
        String[] csvItems = s.substring(1, s.indexOf(']')).split(",");
        if (csvItems.length != 4) {
            throw new CvParameterParserException("This entry contains ONLY " + csvItems.length + " comma separated items");
        }
        CvParameter cvParameter = new CoreCvParameter(csvItems[0].trim(), csvItems[1].trim(), csvItems[2].trim(), csvItems[3].trim());
        return cvParameter;
    }
}
