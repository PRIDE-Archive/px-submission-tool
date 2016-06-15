package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-14 15:30
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This class defines the interface of a handler that parses the information item in the given line.
 *
 * Information item parsers are organized in a hierarchy by sections and line items themselves, at the leaves of each
 * hierarchy branch, one or more processing strategies will be found.
 *
 * A particular context strategy will arrange the processing strategies in a "chain of responsibility" manner.
 */

public abstract class LineItemParsingHandler {
    private static final Logger logger = LoggerFactory.getLogger(LineItemParsingHandler.class);

    // Next handler
    private LineItemParsingHandler nextHandler;

    public LineItemParsingHandler() {
        nextHandler = null;
    }

    public LineItemParsingHandler(LineItemParsingHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    protected LineItemParsingHandler getNextHandler() {
        return nextHandler;
    }

    public void setNextHandler(LineItemParsingHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    // Chain of Responsibility for parsing a particular mzTab line
    public boolean parseLine(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        if (!doParseLine(context, line, lineNumber, offset)) {
            return (getNextHandler() != null) ? getNextHandler().parseLine(context, line, lineNumber, offset) : false;
        }
        return true;
    }

    // Delegate
    protected abstract boolean doParseLine(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
