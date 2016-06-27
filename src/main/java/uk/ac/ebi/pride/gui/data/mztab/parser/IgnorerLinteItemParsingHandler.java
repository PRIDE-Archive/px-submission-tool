package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-27 11:38
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This line item parsing handler, processes a line item by just ignoring the line
 */
public class IgnorerLinteItemParsingHandler extends LineItemParsingHandler {
    @Override
    protected boolean doParseLine(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        return true;
    }
}
