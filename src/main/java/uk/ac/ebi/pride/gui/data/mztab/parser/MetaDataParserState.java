package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.ParserStateException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-09 1:46
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 * <p>
 * This class defines some parts of a ParserState that corresponds to the metadata section of the mzTab file
 *
 * Every mzTab line is tagged with the section it belongs to, and every section will contain different elements of information
 * that need to be processed in a different way.
 *
 */
public abstract class MetaDataParserState extends ParserState {
    private static final Logger logger = LoggerFactory.getLogger(MetaDataParserState.class);

    private static final String STATE_ID_NAME = "MetaData_parser_state";

    @Override
    protected String getStateIdName() {
        return STATE_ID_NAME;
    }

    @Override
    public void parseLine(MzTabParser context, String line, long lineNumber, long offset) throws ParserStateException {
        // Routing algorithm at Section level
        if (line.startsWith("MTD") || line.startsWith("COM")) {
            // Get appropiate section item parser
            try {
                if (!getLineItemParsingHandler().parseLine(context, line, lineNumber, offset)) {
                    logger.warn("IGNORED Line '" + lineNumber + "', offset '" + offset + "', content '" + line + "'");
                }
            } catch (LineItemParsingHandlerException e) {
                throw new ParserStateException("Error parsing line '" + lineNumber + "' ---> " + e.getMessage());
            }
        } else if (line.startsWith("PRH")) {
            // TODO Change state to parsing Proteins
        } else if (line.startsWith("PSH")) {
            // TODO Change state to parsing PSMs
        } else if (line.startsWith("PEH")) {
            // TODO Change state to parsing Peptides
        } else if (line.startsWith("SMH")) {
            // TODO Change state to parsing Small Molecules
        } else {
            // UNEXPECTED Line content ERROR
            throw new ParserStateException("UNEXPECTED LINE '" + line + "' at line number '" + lineNumber + "', offset '" + offset + "'");
        }
    }
}
