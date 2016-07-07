package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.ParsingHelperException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 14:22
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This class contains a collection of parsing helpers that are useful for other parts of the mzTab parser
 */
public class ParsingHelper {
    /**
     * Get the index from an entry like <keyword[index]>
     * @param keyword keyword to match
     * @param token String where to parse the index out of
     * @return the index or -1 if it could not be found
     * @throws ParsingHelperException when the token doesn't comply with the mentioned format
     */
    public static int getIndexInSquareBracketsFromIndexedKeyword(String keyword, String token) throws ParsingHelperException {
        try {
            String key = token.substring(0, token.indexOf("["));
            if (key.equals(keyword)) {
                return Integer.valueOf(token.substring(token.indexOf("[") + 1, token.indexOf("]")));
            }
        } catch (Exception e) {
            throw new ParsingHelperException(e.getMessage());
        }
        // Something weird happened
        return -1;
    }
}
