package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 12:15
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * Quick processing strategy for ms-run location entries
 */

public class QuickMzTabMsRunLocationLineItemParsingHandler extends MzTabMsRunLocationLineItemParsingHandler {
    private void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (getMsRunFromContext(context, getIndex()).getLocation() != null) {
            throw new LineItemParsingHandlerException("DUPLICATED MS-Run location entry FOUND AT LINE " + lineNumber);
        }
    }

    @Override
    protected boolean doProcessEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        checkForDuplicatedEntry(context, lineNumber);
        // Process entry
        try {
            context.getMetaDataSection().getMsRunEntry(getIndex()).setLocation(new URL(getPropertyValue()));
        } catch (MalformedURLException e) {
            throw new LineItemParsingHandlerException(e.getMessage());
        }
        return true;
    }
}
