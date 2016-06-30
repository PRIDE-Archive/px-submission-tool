package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.*;
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
 *
 * This particular implementation, already is a "file based" mzTab parser, for scope reasons
 */
public abstract class MzTabParser {
    private static final Logger logger = LoggerFactory.getLogger(MzTabParser.class);

    // Final Product
    private MzTabDocument mzTabDocument;

    // Parser State Delegate
    private ParserState parserState;

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
    // Return the product of this statefull builder
    public MzTabDocument getMzTabDocument() {
        // In the context of a parser starting from scratch, it makes sense that we create an new MzTabDocument if none
        // has been set up
        if (mzTabDocument == null) {
            logger.debug("Creating a new mzTab document");
            mzTabDocument = new MzTabDocument();
        }
        return mzTabDocument;
    }

    // Director of the parsing process
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
    protected abstract StrategyParserStateFactory getParserStateFactory();

    /**
     * Several object hierarchies have been designed and implemented here to achieve low coupling, flexibility, extendability,
     * etc. But, at some point you need to stop putting levels of indirection, otherwise, your code could grow exponentially.
     *
     * A default "doParse" implementation is provided, working on local files, which is the use case that this solution is
     * trying covering, if, in the future, the software is required to read mzTab files from sources other than files or,
     * a different parsing needs to be done on mzTab files, the following method could be made abstract, delegating on subclasses
     * the implementation of the top level parsign algorithm / strategy for mzTab formatted data
     *
     * This is a director algorithm for building the mzTab Document product, parserState implements State and Builder pattern
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
                throw new MzTabParserException("Error parsing the mzTab file\n" + e.getMessage());
            }
            if (positionAwareLine != null) {
                // Parse the line
                parserState.parseLine(this, positionAwareLine.getLine(),
                        positionAwareLine.getLineNo(),
                        positionAwareLine.getOffset());
            } else {
                // We reached the end of the stream
                break;
            }
        }
    }

    /**
     * This is another method that could be delegated to subclasses, if another kind of mzTab parser wants to be implemented
     */
    protected void doInitParser() {
        // Create a new mzTab Document
        if (getMzTabDocument() == null) setMzTabDocument(new MzTabDocument());
        // Set initial state
        parserState = getParserStateFactory().getMetaDataParserState();
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

    // Subproducts management code
    // MetaData Section
    public MetaData getMetaDataSection() {
        MetaData metaData = getMzTabDocument().getMetaData();
        if (metaData == null) {
            // In the context of a parser processing an mzTab document, it makes sense to create an empty meta data
            // section the first time the parser refers to it, if no previous meta data section was set
            logger.debug("Creating new metadata section for the mzTab document");
            metaData = new MetaData();
            getMzTabDocument().setMetaData(metaData);
        }
        return metaData;
    }
    // Protein Data manager (section)
    public ProteinData getProteinDataSection() {
        ProteinData proteinData = getMzTabDocument().getProteinData();
        if (proteinData == null) {
            // Create an empty protein data section
            proteinData = new ProteinData();
            // for this document
            getMzTabDocument().setProteinData(proteinData);
        }
        return proteinData;
    }

    // Peptide Data manager (section)
    public PeptideData getPeptideDataSection() {
        PeptideData peptideData = getMzTabDocument().getPeptideData();
        if (peptideData == null) {
            peptideData = new PeptideData();
            getMzTabDocument().setPeptideData(peptideData);
        }
        return peptideData;
    }

    // PSM Data manager (section)
    public PsmData getPsmDataSection() {
        PsmData psmData = getMzTabDocument().getPsmData();
        if (psmData == null) {
            psmData = new PsmData();
            getMzTabDocument().setPsmData(psmData);
        }
        return psmData;
    }

    // Small Molecule Data manager (section)
    public SmallMoleculeData getSmallMoleculeDataSection() {
        SmallMoleculeData smallMoleculeData = getMzTabDocument().getSmallMoleculeData();
        if (smallMoleculeData == null) {
            smallMoleculeData = new SmallMoleculeData();
            getMzTabDocument().setSmallMoleculeData(smallMoleculeData);
        }
        return smallMoleculeData;
    }

}
