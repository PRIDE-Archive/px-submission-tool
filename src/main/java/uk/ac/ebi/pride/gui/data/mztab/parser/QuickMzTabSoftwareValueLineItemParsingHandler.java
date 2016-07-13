package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 16:44
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabSoftwareValueLineItemParsingHandler extends MzTabSoftwareValueLineItemParsingHandler {
    private void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (getSoftwareEntryFromContext(context, getIndex()).getValue() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED entry for software at line '" + lineNumber + "'");
        }
    }

    @Override
    protected boolean doProcessEntry(MzTabParser context, long lineNumber, long offset)  throws LineItemParsingHandlerException {
        checkForDuplicatedEntry(context, lineNumber);
        getSoftwareEntryFromContext(context, getIndex()).setValue(CvParameterParser.fromString(getPropertyValue()));
        return true;
    }
}
