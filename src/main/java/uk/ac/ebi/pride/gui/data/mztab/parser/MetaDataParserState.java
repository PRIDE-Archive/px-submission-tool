package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.InvalidMzTabSectionException;
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
    protected boolean doValidateSubProduct(MzTabParser context) throws ParserStateException {
        // Delegate validation to the product itself, for the given context
        try {
            return context.getMetaDataSection().validate(context.getMzTabDocument(), context.getMzTabSectionValidator());
        } catch (InvalidMzTabSectionException e) {
            throw new ParserStateException("Invalid validation of Meta Data section due to:\n" + e.getMessage());
        }
    }

    @Override
    public void parseLine(MzTabParser context, String line, long lineNumber, long offset) throws ParserStateException {
        // TODO - Remove the beginning ("MTD") of the line, as line item parsers don't introduce redundancy by checking
        // TODO - this is present, and they should only deal with the rest of the line, where the fact that we are in
        // TODO - the right section of the file, has already been checked by the corresponding ParserState object
        // Routing algorithm at Section level
        if (!line.isEmpty()) {
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
                // Change state to parsing Proteins
                ProteinParserState proteinParserState = context.getParserStateFactory().getProteinParserState();
                // Update the state
                changeState(context, proteinParserState);
                // Call the parser
                proteinParserState.parseLine(context, line, lineNumber, offset);
            } else if (line.startsWith("PSH")) {
                // Change state to parsing PSMs
                PsmParserState psmParserState = context.getParserStateFactory().getPsmParserState();
                changeState(context, psmParserState);
                psmParserState.parseLine(context, line, lineNumber, offset);
            } else if (line.startsWith("PEH")) {
                // Change state to parsing Peptides
                PeptideParserState peptideParserState = context.getParserStateFactory().getPeptideParserState();
                changeState(context, peptideParserState);
                peptideParserState.parseLine(context, line, lineNumber, offset);
            } else if (line.startsWith("SMH")) {
                // Change state to parsing Small Molecules
                SmallMoleculeParserState smallMoleculeParserState = context.getParserStateFactory().getSmallMoleculeParserState();
                changeState(context, smallMoleculeParserState);
                smallMoleculeParserState.parseLine(context, line, lineNumber, offset);
            } else {
                // UNEXPECTED Line content ERROR
                throw new ParserStateException("UNEXPECTED LINE '" + line + "' at line number '" + lineNumber + "', offset '" + offset + "'");
            }
        }
    }
}
