package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.MzTabDocument;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-08 21:25
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This class implements the mzTab file parser context, for a statefull parser of the different sections in the file
 */
public abstract class MzTabParser {
    private static final Logger logger = LoggerFactory.getLogger(MzTabParser.class);

    // Final Product
    private MzTabDocument mzTabDocument;

    // Parser State Delegate
    private ParserState parserState;

    protected MzTabParser() {
        mzTabDocument = null;
        parserState = null;
    }

    protected void setMzTabDocument(MzTabDocument mzTabDocument) {
        this.mzTabDocument = mzTabDocument;
    }
    protected void setParserState(ParserState parserState) {
        this.parserState = parserState;
    }
    // Get the Strategy ParserState Factory
    protected abstract StrategyParserStateFactory getParserFactory();

    // Return the product of this statefull builder
    public MzTabDocument getMzTabDocument() {
        return mzTabDocument;
    }

    // Director of the parsing process (build)
    public final void parse() {
        doParse();
        doValidateProduct();
    }

    protected void changeState(ParserState newState) {
        logger.debug("Changing state '" + parserState.getStateIdName()
                + "' to new State '" + newState.getStateIdName() + "'");
        parserState = newState;
    }

    // Delegate steps
    protected abstract void doParse();
    protected abstract void doValidateProduct();

}
