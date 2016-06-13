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
 * This class defines some parts of a ParserState that corresponds to the metadata section of the mzTab file
 */
public abstract class MetaDataParserState extends ParserState {
    private static final Logger logger = LoggerFactory.getLogger(MetaDataParserState.class);

    private static final String STATE_ID_NAME = "MetaData_parser_state";

    @Override
    protected String getStateIdName() {
        return STATE_ID_NAME;
    }

    @Override
    public void parseLine(MzTabParser context, String line, long lineNumber, long offset) {
        // Routing algorithm at Section level
        if (line.startsWith("MTD")) {
            // TODO Get appropiate section item parser
        } else if (line.startsWith("COM")) {
            // TODO Get comment parsing strategy (context aware)
        } else if (line.startsWith("PRH")) {
            // TODO Change state to parsing Proteins
        } else if (line.startsWith("PSH")) {
            // TODO Change state to parsing PSMs
        } else if (line.startsWith("PEH")) {
            // TODO Change state to parsing Peptides
        } else if (line.startsWith("SMH")) {
            // TODO Change state to parsing Small Molecules
        } else {
            // TODO UNEXPECTED Line content ERROR
        }
    }
}
