package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.MetaData;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-09 11:37
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * Different ParserState factories will encapsulate different parser strategies
 */

public interface StrategyParserStateFactory {

    // Get a parser for the MetaData section
    MetaDataParserState getMetaDataParserState();
}
