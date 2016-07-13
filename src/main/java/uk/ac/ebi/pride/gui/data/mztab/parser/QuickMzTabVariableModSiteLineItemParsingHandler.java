package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 11:11
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabVariableModSiteLineItemParsingHandler extends MzTabVariableModSiteLineItemParsingHandler {
    private void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (getVariableModFromContext(context, getIndex()).getSite() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED entry for variable_mod site entry FOUND AT LINE " + lineNumber);
        }
    }

    @Override
    protected boolean doProcessEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        checkForDuplicatedEntry(context, lineNumber);
        getVariableModFromContext(context, getIndex()).setSite(getPropertyValue());
        return true;
    }
}
