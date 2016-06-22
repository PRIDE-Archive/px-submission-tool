package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 11:50
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * Check that the given ms-run entry corresponds to an id_format entry
 *
 * It delegates further processing of its data
 */

public abstract class MzTabMsRunIdFormatLineItemParsingHandler extends MzTabMsRunLineItemParsingHandler {
    private static final String MZTAB_MSRUN_ID_FORMAT_PROPERTY_KEY = "id_format";

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset)  throws LineItemParsingHandlerException {
        if (getPropertyKey().equals(MZTAB_MSRUN_ID_FORMAT_PROPERTY_KEY)) {
            // Go ahead
            return doProcessEntry(context, lineNumber, offset);
        }
        return false;
    }

    // Delegate to strategy subclasses what to do
    protected abstract boolean doProcessEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException ;
}
