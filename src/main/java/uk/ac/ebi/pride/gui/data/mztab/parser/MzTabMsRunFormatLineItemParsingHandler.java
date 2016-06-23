package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 11:10
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * It checks the ms-run entry to be of type format
 *
 * It delegates further processing of its data
 */

public abstract class MzTabMsRunFormatLineItemParsingHandler extends MzTabMsRunLineItemParsingHandler {
    private static final Logger logger = LoggerFactory.getLogger(MzTabMsRunFormatLineItemParsingHandler.class);
    protected static final String MZTAB_MSRUN_FORMAT_PROPERTY_KEY = "format";

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset)  throws LineItemParsingHandlerException {
        if (getPropertyKey().equals(MZTAB_MSRUN_FORMAT_PROPERTY_KEY)) {
            return doProcessEntry(context, lineNumber, offset);
        }
        logger.debug("Found property key '" + getPropertyKey() + "' but this parser is expecting '" + MZTAB_MSRUN_FORMAT_PROPERTY_KEY + "'");
        return false;
    }

    // Delegate to strategy subclasses what to do
    protected abstract boolean doProcessEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException ;
}
