package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-14 16:27
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class QuickMzTabModeLineItemParsingHandler extends MzTabModeLineItemParsingHandler {
    private static final Logger logger = LoggerFactory.getLogger(QuickMzTabModeLineItemParsingHandler.class);

    @Override
    protected boolean doProcessCompleteMode(MzTabParser context, String line, long lineNumber, long offset) {
        // TODO
        return false;
    }

    @Override
    protected boolean doProcessSummaryMode(MzTabParser context, String line, long lineNumber, long offset) {
        // TODO
        return false;
    }
}
