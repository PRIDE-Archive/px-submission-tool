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

    // Chain of responsibility for parsing metadata items
    private LineItemParsingHandler lineItemParsingHandler = null;

    public QuickMetaDataParserState() {
        // Lazy building of the chain of responsibility that takes care of parsing the meta data section
    }

    private void buildLineItemParsingHandlerChain() {
        // If we wanted more flexibility here, we could add another layer of indirection by externalizing the creation
        // of LineItemParsingHandlers
        // This way of implementing a combination of Chain of Responsibility, Builder and Abstract Factory, gives us
        // complete freedom to combine all sorts of final processing strategies for the line items, in a way that we can
        // quickly change from ignoring a particular line entry to process it by choosing a different processing
        // strategy among any of the available ones, globally
        // TODO
    }

    @Override
    protected LineItemParsingHandler getLineItemParsingHandler() {
        if (lineItemParsingHandler == null) {
            buildLineItemParsingHandlerChain();
        }
        return lineItemParsingHandler;
    }

    @Override
    protected void doChangeState(MzTabParser context, ParserState newState) {
        // TODO
    }
}
