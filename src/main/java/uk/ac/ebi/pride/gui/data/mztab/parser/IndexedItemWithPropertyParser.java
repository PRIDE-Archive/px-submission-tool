package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.IndexedItemWithPropertyParserException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 14:26
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class IndexedItemWithPropertyParser {
    public static boolean parseLine(MetaDataLineItemParsingHandler.IndexedItemWithProperty bean, String line) throws IndexedItemWithPropertyParserException {
        String[] lineItems = line.split("\t");
        try {
            if (lineItems.length == 3) {
                // Extract item index
                bean.setIndex(Integer.valueOf(lineItems[1].substring(lineItems[1].indexOf('[') + 1, lineItems[1].indexOf(']'))));
                bean.setPropertyKey(lineItems[1].substring(lineItems[1].indexOf(']') + 2).trim());
                bean.setPropertyValue(lineItems[2].trim());
                return true;
            }
        } catch (NumberFormatException e) {
            throw new IndexedItemWithPropertyParserException(e.getMessage());
        }
        return false;
    }

    // No instances of this class

    private IndexedItemWithPropertyParser() {
    }
}
