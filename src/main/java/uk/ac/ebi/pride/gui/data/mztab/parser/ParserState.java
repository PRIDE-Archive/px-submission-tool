package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-08 21:41
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class ParserState {
    private static final Logger logger = LoggerFactory.getLogger(ParserState.class);

    // Parse a line
    public abstract void parseLine(MzTabParser context, String line, long lineNumber, long offset);
    // Change state delegate
    protected final void changeState(MzTabParser context, ParserState newState) {
        doValidateProduct(context);
        doSetProduct(context);
        doChangeState(context, newState);
    }
    // Delegate to subclasses
    protected abstract void doValidateProduct(MzTabParser context);
    protected abstract void doSetProduct(MzTabParser context);
    protected abstract void doChangeState(MzTabParser context, ParserState newState);

}
