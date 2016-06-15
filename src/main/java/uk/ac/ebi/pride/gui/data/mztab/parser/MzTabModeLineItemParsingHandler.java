package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

import java.util.List;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-14 16:14
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public abstract class MzTabModeLineItemParsingHandler extends MetaDataLineItemParsingHandler {
    private static final Logger logger = LoggerFactory.getLogger(MzTabModeLineItemParsingHandler.class);

    protected static final String MZTAB_MODE_KEYWORD = "mzTab-mode";
    protected static final String MODE_COMPLETE_KEYWORD = "Complete";
    protected static final String MODE_SUMMARY_KEYWORD = "Summary";

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        String[] lineItems = line.split("\t");
        if ((lineItems.length == 3)
                && (lineItems[1].equals(MZTAB_MODE_KEYWORD))) {
            if (lineItems[2].equals(MODE_COMPLETE_KEYWORD)) {
                return doProcessCompleteMode(context, line, lineNumber, offset);
            } else if (lineItems[2].equals(MODE_SUMMARY_KEYWORD)) {
                return doProcessSummaryMode(context, line, lineNumber, offset);
            } else {
                throw new LineItemParsingHandlerException("mzTab mode '" + lineItems[2] + "' NOT RECOGNIZED");
            }
        }
        return false;
    }

    // Process mzTab mode "Complete"
    protected abstract boolean doProcessCompleteMode(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException ;
    // Process mzTab mode "Summary"
    protected abstract boolean doProcessSummaryMode(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException ;
}
