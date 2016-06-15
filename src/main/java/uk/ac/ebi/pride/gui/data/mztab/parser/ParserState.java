package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.ParserStateException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-08 21:41
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * The hierarchy ParserState implements a State pattern, changing the behaviour of the parser at runtime,
 * depending on the particular section that is currently being processed
 *
 */
public abstract class ParserState {
    private static final Logger logger = LoggerFactory.getLogger(ParserState.class);

    public abstract void parseLine(MzTabParser context, String line, long lineNumber, long offset) throws ParserStateException;
    // Change state delegate
    protected final void changeState(MzTabParser context, ParserState newState) {
        doValidateProduct(context);
        doSetSubProduct(context);
        doChangeState(context, newState);
    }
    // Delegate to subclasses
    // Line item parsing handler
    protected abstract LineItemParsingHandler getLineItemParsingHandler();
    protected abstract void doValidateProduct(MzTabParser context);
    protected abstract void doSetSubProduct(MzTabParser context);
    protected abstract void doChangeState(MzTabParser context, ParserState newState);
    // Get this state ID name
    protected abstract String getStateIdName();
}
