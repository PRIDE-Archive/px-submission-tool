package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.exceptions.InvalidMzTabDocument;
import uk.ac.ebi.pride.gui.data.mztab.model.MetaData;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-08 23:52
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This is a quick parser for mzTab files, it will get
 */
public class MzTabFullDocumentQuickParser extends MzTabParser {
    private static final Logger logger = LoggerFactory.getLogger(MzTabFullDocumentQuickParser.class);

    // Parsing Strategy Factory
    private static final StrategyParserStateFactory PARSER_STATE_FACTORY = new QuickParserStrategyFactory();

    public MzTabFullDocumentQuickParser(String fileName) {
        super(fileName);
    }

    @Override
    protected StrategyParserStateFactory getParserStateFactory() {
        return PARSER_STATE_FACTORY;
    }

    @Override
    public MetaData getMetaDataSection() {
        // When one of the subproducts of the final product is accessed, as we are parsing the document from scartch,
        // it is our responsibility to make sure the empty one is available for the different subproduct builders.
        if (super.getMetaDataSection() == null) {
            setMetaDataSection(new MetaData());
        }
        return super.getMetaDataSection();
    }
}
