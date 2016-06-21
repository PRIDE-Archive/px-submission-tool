package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 11:50
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public abstract class MzTabMsRunIdFormatLineItemParsingHandler extends MzTabMsRunLineItemParsingHandler {
    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset)  throws LineItemParsingHandlerException {
        return doProcessEntry(context, lineNumber, offset);
    }

    // Delegate to strategy subclasses what to do
    protected abstract boolean doProcessEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException ;
}
