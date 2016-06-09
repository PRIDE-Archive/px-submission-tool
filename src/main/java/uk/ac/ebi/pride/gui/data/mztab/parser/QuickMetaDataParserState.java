package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-09 14:28
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This class models a quick parser for mzTab metadata, that will collect just the information we need in the
 * px-submission-tool to collect related files and automatically annotate the submission process
 */

public class QuickMetaDataParserState extends MetaDataParserState {
    private static final Logger logger = LoggerFactory.getLogger(QuickMetaDataParserState.class);

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
}
