package uk.ac.ebi.pride.gui.data.mztab.parser;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-30 14:26
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickPeptideParserState extends PeptideParserState {
    @Override
    protected LineItemParsingHandler buildLineItemParsingHandlerChain() {
        // Make the list of handlers
        LineItemParsingHandler[] handlers = new LineItemParsingHandler[] {
                new QuickPeptideDataHeaderLineItemParsingHandler(),
                new QuickPeptideDataEntryLineItemParsingHandler(),
                new IgnorerLinteItemParsingHandler()
        };
        // Build the chain of responsibility
        for (int i = 0; i < (handlers.length - 1); i++) {
            handlers[i].setNextHandler(handlers[i + 1]);
        }
        // Return the start of the chain
        return handlers[0];
    }
}
