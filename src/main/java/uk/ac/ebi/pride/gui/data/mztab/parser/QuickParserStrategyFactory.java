package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-09 14:44
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This Factory puts together a collection of ParserState objects for the different sections of mzTab files, that as a
 * team, implement the "quick file parsing strategy", which is about collecting just the information we need from the
 * mzTab file for the px-submission tool
 */

public class QuickParserStrategyFactory implements StrategyParserStateFactory {
    private static final Logger logger = LoggerFactory.getLogger(QuickParserStrategyFactory.class);

    @Override
    public MetaDataParserState getMetaDataParserState() {
        return new QuickMetaDataParserState();
    }
}
