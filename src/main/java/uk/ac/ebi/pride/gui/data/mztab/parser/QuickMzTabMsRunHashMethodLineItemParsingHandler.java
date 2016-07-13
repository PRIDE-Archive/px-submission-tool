package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.MsRunHashMethod;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 22:30
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabMsRunHashMethodLineItemParsingHandler extends MzTabMsRunHashMethodLineItemParsingHandler {
    private void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (getMsRunFromContext(context, getIndex()).getHashMethod() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED hash_method entry for ms_run at line '" + lineNumber + "'");
        }
    }

    @Override
    protected boolean doProcessEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        checkForDuplicatedEntry(context, lineNumber);
        getMsRunFromContext(context, getIndex()).setHashMethod(new MsRunHashMethod(CvParameterParser.fromString(getPropertyValue())));
        return true;
    }
}
