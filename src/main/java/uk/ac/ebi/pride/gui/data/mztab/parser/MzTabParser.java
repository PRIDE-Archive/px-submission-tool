package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.MzTabDocument;
import uk.ac.ebi.pride.gui.data.mztab.parser.readers.LineAndPositionAwareBufferedReader;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.MzTabParserException;

import java.io.IOException;

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
    private StrategyParserStateFactory strategyParserStateFactory;

    // Source mzTab file
    private String fileName;

    protected MzTabParser(String fileName) {
        mzTabDocument = null;
        parserState = null;
        this.fileName = fileName;
    }

    protected void setMzTabDocument(MzTabDocument mzTabDocument) {
        this.mzTabDocument = mzTabDocument;
    }
    protected void setParserState(ParserState parserState) {
        this.parserState = parserState;
    }
    protected StrategyParserStateFactory getStrategyParserStateFactoryFactory() {
        return strategyParserStateFactory;
    }

    // Return the product of this statefull builder
    public MzTabDocument getMzTabDocument() {
        return mzTabDocument;
    }

    // Director of the parsing process (build)
    public final void parse() {
        if (getMzTabDocument() != null) {
            logger.error("This document has already been parsed!");
            return;
        } else {
            doInitParser();
        }
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
    protected void doParse() throws MzTabParserException {
        // check file access
        // open file
        LineAndPositionAwareBufferedReader reader = null;
        try {
            reader = new LineAndPositionAwareBufferedReader(fileName);
        } catch (IOException e) {
            throw new MzTabParserException("Could not start mzTab parser\n" + e.toString());
        }
        // Parse the file (Section Routing Algorithm)
        while (true) {
            LineAndPositionAwareBufferedReader.PositionAwareLine positionAwareLine = null;
            try {
                positionAwareLine = reader.readLine();
            } catch (IOException e) {
                throw new MzTabParserException("Error parsing the mzTab file\n" + e.toString());
            }
            if (positionAwareLine != null) {
                // Parse the line
                parserState.parseLine(this, positionAwareLine.getLine(),
                        positionAwareLine.getLineNo(),
                        positionAwareLine.getOffset());
            }
            break;
        }
    }

    /**
     * This is another method that could be delegated to subclasses, if another kind of mzTab parser wants to be implemented
     */
    protected void doInitParser() {
        // Create a new mzTab Document
        setMzTabDocument(new MzTabDocument());
        // Set initial state
        parserState = getStrategyParserStateFactoryFactory().getMetaDataParserState();
    }

    // Product validation by delegation
    protected void doValidateProduct() {
        if (getMzTabDocument() != null) {
            if (!getMzTabDocument().validate()) {
                throw new MzTabParserException("The parsed mzTab document IS NOT VALID!");
            }
        } else {
            throw new MzTabParserException("There is no mzTab document to validate!");
        }
    }

}
