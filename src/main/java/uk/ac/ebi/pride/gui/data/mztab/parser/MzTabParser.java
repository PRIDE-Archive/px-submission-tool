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

    // Parser State Factory
    StrategyParserStateFactory strategyParserStateFactory;

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
    protected StrategyParserStateFactory getParserFactory() {
        return strategyParserStateFactory;
    }

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
    /**
     * Several object hierarchies have been designed and implemented here to achieve low coupling, flexibility, extendability,
     * etc. But, at some point you need to stop putting levels of indirection, otherwise, your code could grow exponentially.
     *
     * A default "doParse" implementation is provided, working on local files, which is the use case that this solution is
     * trying covering, if, in the future, the software is required to read mzTab files from sources other than files or,
     * a different parsing needs to be done on mzTab files, the following method could be made abstract, delegating on subclasses
     * the implementation of the top level parsign algorithm / strategy for mzTab formatted data
     */
    protected void doParse() {
        // TODO check file access
        // TODO open file
        // TODO use a line number and position aware BufferedReader
    }
    protected abstract void doValidateProduct();

}
