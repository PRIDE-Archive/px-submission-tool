package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-28 12:22
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 * <p>
 * This branch of the hierarchy processes protein data
 */
public abstract class ProteinDataLineItemParsingHandler extends LineItemParsingHandler {
    @Override
    protected boolean doParseLine(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        return doParseLineItem(context, line, lineNumber, offset);
    }

    // Delegate processing of a particular item to subclass
    protected abstract boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
