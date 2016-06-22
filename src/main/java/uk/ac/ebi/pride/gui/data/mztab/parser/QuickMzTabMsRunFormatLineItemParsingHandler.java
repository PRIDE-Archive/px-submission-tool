package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.MsRunFormat;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 11:12
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * Quick processing strategy for ms-run format entries
 */

public class QuickMzTabMsRunFormatLineItemParsingHandler extends MzTabMsRunFormatLineItemParsingHandler {
    private static final Logger logger = LoggerFactory.getLogger(QuickMzTabMsRunFormatLineItemParsingHandler.class);

    // Check for duplicated entry
    public void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (getMsRunFromContext(context, getIndex()).getMsRunFormat() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED MS-Run format entry FOUND AT LINE " + lineNumber);
        }
    }

    @Override
    protected boolean doProcessEntry(MzTabParser context, long lineNumber, long offset) {
        checkForDuplicatedEntry(context, lineNumber);
        // Process the entry
        getMsRunFromContext(context, getIndex()).setMsRunFormat(new MsRunFormat(CvParameterParser.fromString(getPropertyValue())));
        return true;
    }
}
