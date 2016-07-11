package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 10:04
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class MzTabFixedModValueLineItemParsingHandler extends MzTabFixedModLineItemParsingHandler {
    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        if (getPropertyKey().equals(DEFAULT_PROPERTY_KEY)) {
            // This means no property key has been found when parsing this entry, thus, this contains the top value for
            // a FixedMod object
            return doProcessEntry(context, lineNumber, offset);
        }
        return false;
    }

    // Delegate processing strategy
    protected abstract boolean doProcessEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
