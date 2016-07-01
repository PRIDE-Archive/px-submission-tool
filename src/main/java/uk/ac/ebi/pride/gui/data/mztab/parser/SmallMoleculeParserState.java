package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.InvalidMzTabSectionException;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.ParserStateException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-30 13:18
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This class defines the behaviour when working on mzTab Small Molecule Data
 */
public abstract class SmallMoleculeParserState extends ParserState {
    private static final Logger logger = LoggerFactory.getLogger(SmallMoleculeParserState.class);

    private static final String STATE_ID_NAME = "Small_Molecule_parser_state";

    @Override
    protected String getStateIdName() {
        return STATE_ID_NAME;
    }

    @Override
    protected boolean doValidateSubProduct(MzTabParser context) throws ParserStateException {
        // Delegate validation to the subproduct itself, for the given context
        try {
            return context.getSmallMoleculeDataSection().validate(context.getMzTabDocument(), context.getMzTabSectionValidator());
        } catch (InvalidMzTabSectionException e) {
            throw new ParserStateException("Invalid validation of Small Molecule Data section due to ---> " + e.getMessage());
        }
    }

    @Override
    public void parseLine(MzTabParser context, String line, long lineNumber, long offset) throws ParserStateException {
        if (!line.isEmpty()) {
            if (line.matches("^(SMH|SML|COM)\\s?")) {
                // We process header, data entries or comments
                try {
                    if (!getLineItemParsingHandler().parseLine(context, line, lineNumber, offset)) {
                        logger.warn("IGNORED Line '" + lineNumber + "', offset '" + offset + "', content '" + line + "'");
                    }
                } catch (LineItemParsingHandlerException e) {
                    throw new ParserStateException("Error parsing line '" + lineNumber + "' ---> " + e.getMessage());
                }
            } else if (line.startsWith("PRH")) {
                // Change state to Protein processing mode
                ProteinParserState proteinParserState = context.getParserStateFactory().getProteinParserState();
                changeState(context, proteinParserState);
                proteinParserState.parseLine(context, line, lineNumber, offset);
            } else if (line.startsWith("PEH")) {
                // Change state to Peptide processing mode
                PeptideParserState peptideParserState = context.getParserStateFactory().getPeptideParserState();
                changeState(context, peptideParserState);
                peptideParserState.parseLine(context, line, lineNumber, offset);
            } else if (line.startsWith("PSH")) {
                // Change state to PSM processing mode
                PsmParserState psmParserState = context.getParserStateFactory().getPsmParserState();
                changeState(context, psmParserState);
                psmParserState.parseLine(context, line, lineNumber, offset);
            } else {
                // UNEXPECTED Line content ERROR
                throw new ParserStateException("UNEXPECTED LINE '" + line + "' at line number '" + lineNumber + "', offset '" + offset + "'");
            }
        }
    }
}
