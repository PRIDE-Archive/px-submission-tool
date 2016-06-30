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
        doValidateSubProduct(context);
        doChangeState(context, newState);
    }

    protected void doChangeState(MzTabParser context, ParserState newState) {
        // The responsibilities described for this quick parser at this time, it makes sense that all the different
        // strategies will do the same when it comes to change the state of the parser, as new parser states are created
        // via an abstract factory, and no special/additional steps are required to change the state.
        // Nonetheless, I leave this method here as a "landmark", to point at where special and/or additional steps
        // should be taken upon state change. This gives the software both a general algorithm and a fine tune point,
        // both super classes and subclasses a "say" upon a state change. Any additional processing upon state change
        // will push this method down the class hierarchy
        // TODO
    }
    // Delegate to subclasses
    // Line item parsing handler
    protected abstract LineItemParsingHandler getLineItemParsingHandler();
    protected abstract void doValidateSubProduct(MzTabParser context);
    // Get this state ID name
    protected abstract String getStateIdName();
}
