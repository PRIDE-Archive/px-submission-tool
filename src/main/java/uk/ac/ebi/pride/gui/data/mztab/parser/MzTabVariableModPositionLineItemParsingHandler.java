package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 11:09
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class MzTabVariableModPositionLineItemParsingHandler extends MzTabVariableModLineItemParsingHandler {
    protected static final String MZTAB_VARIABLE_MOD_POSITION_PROPERTY_KEY = "position";

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        if (getPropertyKey().equals(MZTAB_VARIABLE_MOD_POSITION_PROPERTY_KEY)) {
            return doProcessEntry(context, lineNumber, offset);
        }
        return false;
    }

    // Delegate processing strategy
    protected abstract boolean doProcessEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
