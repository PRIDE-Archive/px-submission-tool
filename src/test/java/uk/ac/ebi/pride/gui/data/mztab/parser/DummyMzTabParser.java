package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.MzTabSectionValidator;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-15 11:52
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This is a Dummy context for testing line item handlers
 */

public class DummyMzTabParser extends MzTabParser {

    public DummyMzTabParser(String fileName) {
        super(fileName);
    }

    // For testing line item handlers, a parser state factory will never be used
    @Override
    protected StrategyParserStateFactory getParserStateFactory() {
        return null;
    }

    @Override
    protected MzTabSectionValidator getMzTabSectionValidator() {
        return null;
    }
}
