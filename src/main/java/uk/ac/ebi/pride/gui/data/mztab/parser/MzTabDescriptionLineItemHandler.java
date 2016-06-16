package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-16 13:56
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * Parser for collecting mzTab description information
 */

public abstract class MzTabDescriptionLineItemHandler extends MetaDataLineItemParsingHandler {
    // Keyword
    protected static final String MZTAB_DESCRIPTION_KEYWORD = "description";

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        String[] lineItems = line.split("\t");
        if ((lineItems.length == 3) && (lineItems[1].equals(MZTAB_DESCRIPTION_KEYWORD))) {
            return doProcessDescription(context, line, lineNumber, offset, lineItems[2]);
        }
        return false;
    }

    // Delegate processing
    protected abstract boolean doProcessDescription(MzTabParser context, String line, long lineNumber, long offset, String description) throws LineItemParsingHandlerException;
}
