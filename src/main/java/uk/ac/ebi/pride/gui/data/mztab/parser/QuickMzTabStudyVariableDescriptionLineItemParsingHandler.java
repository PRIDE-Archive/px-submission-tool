package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 12:40
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabStudyVariableDescriptionLineItemParsingHandler extends MzTabStudyVariableDescriptionLineItemParsingHandler {
    private void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (getStudyVariableFromContext(context, getIndex()).getDescription() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED description entry for study variable with index '" + getIndex() + "' at line '" + lineNumber + "'");
        }
    }
    @Override
    protected boolean doProcessEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        checkForDuplicatedEntry(context, lineNumber);
        getStudyVariableFromContext(context, getIndex()).setDescription(getPropertyValue());
        return true;
    }
}
