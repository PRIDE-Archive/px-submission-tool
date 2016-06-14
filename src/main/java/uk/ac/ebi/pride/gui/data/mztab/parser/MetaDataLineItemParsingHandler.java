package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-14 16:03
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public abstract class MetaDataLineItemParsingHandler extends LineItemParsingHandler {
    private static final Logger logger = LoggerFactory.getLogger(MetaDataLineItemParsingHandler.class);

    private static final String METADATA_LINE_PREFIX = "MTD";

    public MetaDataLineItemParsingHandler() {
        super();
    }

    public MetaDataLineItemParsingHandler(LineItemParsingHandler nextHandler) {
        super(nextHandler);
    }

    private boolean checkLinePrefix(String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        if (!line.startsWith(METADATA_LINE_PREFIX)) {
            throw new LineItemParsingHandlerException("INVALID LINE PREFIX, expected '" + METADATA_LINE_PREFIX
                    + "', for line '" + line
                    + "', line number '" + lineNumber
                    + "', position '" + offset + "'");
        }
    }
    @Override
    protected boolean doParseLine(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        // Check line Prefix (this is redundant, considering what I've done on other parts of the code
        if (checkLinePrefix(line, lineNumber, offset)) {
            return doParseLineItem(context, line, lineNumber, offset);
        }
        return false;
    }

    // Delegate processing of a particular item to subclass
    protected abstract boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
