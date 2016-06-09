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
 */

public class QuickParserStrategyFactory implements StrategyParserStateFactory {
    private static final Logger logger = LoggerFactory.getLogger(QuickParserStrategyFactory.class);

    @Override
    public MetaDataParserState getMetaDataParserState() {
        return new QuickMetaDataParserState();
    }
}
