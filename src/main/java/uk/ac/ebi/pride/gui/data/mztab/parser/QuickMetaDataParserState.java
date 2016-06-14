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
        // TODO Build the chain of metadata parser items
    }

    private void buildLineItemParsingHandlerChain() {
        // If we wanted more flexibility here, we could add another layer of indirection by externalizing the creation
        // of LineItemParsingHandlers
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
    protected void doValidateProduct(MzTabParser context) {
        // TODO
    }

    @Override
    protected void doSetSubProduct(MzTabParser context) {
        // TODO
    }

    @Override
    protected void doChangeState(MzTabParser context, ParserState newState) {
        // TODO
    }
}
