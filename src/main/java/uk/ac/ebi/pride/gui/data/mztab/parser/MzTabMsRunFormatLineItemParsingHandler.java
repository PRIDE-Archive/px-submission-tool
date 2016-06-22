package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 11:10
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public abstract class MzTabMsRunFormatLineItemParsingHandler extends MzTabMsRunLineItemParsingHandler {
    protected static final String MZTAB_MSRUN_FORMAT_PROPERTY_KEY = "format";

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset)  throws LineItemParsingHandlerException {
        if (getPropertyKey().equals(MZTAB_MSRUN_FORMAT_PROPERTY_KEY)) {
            return doProcessEntry(context, lineNumber, offset);
        }
        return false;
    }

    // Delegate to strategy subclasses what to do
    protected abstract boolean doProcessEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException ;
}
