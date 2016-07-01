package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.MzTabSectionValidator;
import uk.ac.ebi.pride.gui.data.mztab.model.OneTimeDefaultValidatorMzTabSectionValidator;

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

    public MzTabFullDocumentQuickParser(String fileName) {
        super(fileName);
    }

    @Override
    protected StrategyParserStateFactory getParserStateFactory() {
        return new QuickParserStrategyFactory();
    }

    @Override
    protected MzTabSectionValidator getMzTabSectionValidator() {
        return new OneTimeDefaultValidatorMzTabSectionValidator();
    }
}
