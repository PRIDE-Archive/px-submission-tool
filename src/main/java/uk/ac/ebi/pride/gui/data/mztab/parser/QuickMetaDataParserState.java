package uk.ac.ebi.pride.gui.data.mztab.parser;

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
    @Override
    protected LineItemParsingHandler buildLineItemParsingHandlerChain() {
        // If we wanted more flexibility here, we could add another layer of indirection by externalizing the creation
        // of LineItemParsingHandlers
        // This way of implementing a combination of Chain of Responsibility, Builder and Abstract Factory, gives us
        // complete freedom to combine all sorts of final processing strategies for the line items, in a way that we can
        // quickly change from ignoring a particular line entry to process it by choosing a different processing
        // strategy among any of the available ones, globally

        // Make the list of handlers
        LineItemParsingHandler[] handlers = new LineItemParsingHandler[] {
                new QuickMzTabTitleLineItemParsingHandler(),
                new QuickMzTabTypeLineItemParsingHandler(),
                new QuickMzTabVersionLineItemParsingHandler(),
                new QuickMzTabDescriptionLineItemHandler(),
                new QuickMzTabFileIdLineItemParsingHandler(),
                new QuickMzTabModeLineItemParsingHandler(),
                new QuickMzTabMsRunFormatLineItemParsingHandler(),
                new QuickMzTabMsRunIdFormatLineItemParsingHandler(),
                new QuickMzTabMsRunLocationLineItemParsingHandler(),
                new QuickMzTabSampleCellTypeLineItemParsingHandler(),
                new QuickMzTabSampleCustomLineItemParsingHandler(),
                new QuickMzTabSampleDiseaseLineItemParsingHandler(),
                new QuickMzTabSampleSpeciesLineItemParsingHandler(),
                new QuickMzTabSampleTissueLineItemParsingHandler(),
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
