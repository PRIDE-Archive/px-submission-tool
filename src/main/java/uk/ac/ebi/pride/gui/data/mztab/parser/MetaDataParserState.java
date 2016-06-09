package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-09 1:46
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 * <p>
 * This class models the parser state that deals with metadata
 */
public class MetaDataParserState extends ParserState {
    private static final Logger logger = LoggerFactory.getLogger(MetaDataParserState.class);

    private static final String STATE_ID_NAME = "MetaData_parser_state";

    @Override
    public void parseLine(MzTabParser context, String line, long lineNumber, long offset) {
        // TODO
    }

    @Override
    protected void doValidateProduct(MzTabParser context) {
        // TODO
    }

    @Override
    protected void doSetProduct(MzTabParser context) {
        // TODO
    }

    @Override
    protected void doChangeState(MzTabParser context, ParserState newState) {
        // TODO
    }

    @Override
    protected String getStateIdName() {
        return STATE_ID_NAME;
    }
}
