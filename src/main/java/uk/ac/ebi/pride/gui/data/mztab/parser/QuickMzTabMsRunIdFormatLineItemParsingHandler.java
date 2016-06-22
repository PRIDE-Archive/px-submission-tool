package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.MsRunIdFormat;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 11:51
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * Processing strategy for ms-run id_format entries
 */

public class QuickMzTabMsRunIdFormatLineItemParsingHandler extends MzTabMsRunIdFormatLineItemParsingHandler {
    private void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (context.getMetaDataSection().getMsRunEntry(getIndex()).getMsRunIdFormat() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED MS-Run ID Format FOUND AT LINE " + lineNumber);
        }
    }

    @Override
    protected boolean doProcessEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        checkForDuplicatedEntry(context, lineNumber);
        // Process entry
        context.getMetaDataSection().getMsRunEntry(getIndex()).setMsRunIdFormat(new MsRunIdFormat(CvParameterParser.fromString(getPropertyValue())));
        return true;
    }
}
