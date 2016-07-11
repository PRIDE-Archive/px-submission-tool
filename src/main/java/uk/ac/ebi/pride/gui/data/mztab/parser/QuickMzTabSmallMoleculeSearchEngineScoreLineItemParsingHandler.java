package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.SmallMoleculeSearchEngineScore;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-06 11:11
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class QuickMzTabSmallMoleculeSearchEngineScoreLineItemParsingHandler extends MzTabSmallMoleculeSearchEngineScoreLineItemParsingHandler {
    private void checkForDuplicatedEntry(MzTabParser context, long lineNumber) throws LineItemParsingHandlerException {
        if (getSmallMoleculeSearchEngineScoreFromContext(context, getIndex()) != null) {
            throw new LineItemParsingHandlerException("DUPLICATED " + MZTAB_SMALL_MOLECULE_SEARCH_ENGINE_SCORE_KEY + " with index '" + getIndex() + "'");
        }
    }

    @Override
    protected boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException {
        checkForDuplicatedEntry(context, lineNumber);
        setSmallMoleculeSearchEngineScoreInContext(context, getIndex(), new SmallMoleculeSearchEngineScore(CvParameterParser.fromString(getPropertyValue())));
        return true;
    }
}
