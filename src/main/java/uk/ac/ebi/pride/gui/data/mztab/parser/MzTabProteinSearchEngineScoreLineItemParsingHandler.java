package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.ProteinSearchEngineScore;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.MetadataLineItemParserStrategyException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-05 14:51
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class MzTabProteinSearchEngineScoreLineItemParsingHandler extends MetaDataLineItemParsingHandler implements MetaDataLineItemParsingHandler.IndexedLineItemBean {
    private static final Logger logger = LoggerFactory.getLogger(MzTabProteinSearchEngineScoreLineItemParsingHandler.class);

    protected static final String MZTAB_PROTEIN_SEARCH_ENGINE_SCORE_KEY = "protein_search_engine_score";
    // Bean defaults
    protected static final String DEFAULT_LINE_ITEM_KEY = "";
    protected static final int DEFAULT_INDEX = -1;
    protected static final String DEFAULT_PROPERTY_VALUE = "";
    // Bean attributes
    private String lineItemKey = DEFAULT_LINE_ITEM_KEY;
    private int index = DEFAULT_INDEX;
    private String propertyValue = DEFAULT_PROPERTY_VALUE;

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String getLineItemKey() {
        return lineItemKey;
    }

    @Override
    public void setLineItemKey(String lineItemKey) {
        this.lineItemKey = lineItemKey;
    }

    @Override
    public String getPropertyValue() {
        return propertyValue;
    }

    @Override
    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    private void cleanBean() {
        lineItemKey = DEFAULT_LINE_ITEM_KEY;
        index = DEFAULT_INDEX;
        propertyValue = DEFAULT_PROPERTY_VALUE;
    }

    // Working with protein_search_engine_score items
    protected ProteinSearchEngineScore getProteinSearchEngineScoreFromContext(MzTabParser context, int proteinSearchEngineScoreIndex) {
        return context.getMetaDataSection().getProteinSearchEngineScore(proteinSearchEngineScoreIndex);
    }

    protected ProteinSearchEngineScore setProteinSearchEngineScoreInContext(MzTabParser context, int proteinSearchEngineScoreIndex, ProteinSearchEngineScore proteinSearchEngineScore) {
        return context.getMetaDataSection().updateProteinSearchEngineScore(proteinSearchEngineScore, proteinSearchEngineScoreIndex);
    }

    // TODO - Now it is starting to be evident that a refactoring may be useful here to externalize what it looks like a
    // TODO - common line item parsing strategy among similar line items in the metadata section
    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        cleanBean();
        try {
            if (MetadataLineItemParserStrategy.parseLine(this, line)) {
                if (getLineItemKey().equals(MZTAB_PROTEIN_SEARCH_ENGINE_SCORE_KEY)) {
                    // Check that we have a line item index
                    if (getIndex() == DEFAULT_INDEX) {
                        throw new LineItemParsingHandlerException("MISSING line item index for '" + MZTAB_PROTEIN_SEARCH_ENGINE_SCORE_KEY + "'");
                    }
                    // The line item key is ok, go ahead
                    return processEntry(context, lineNumber, offset);
                }
            }
        } catch (MetadataLineItemParserStrategyException e) {
            throw new LineItemParsingHandlerException(e.getMessage());
        }
        return false;

    }

    // Delegate processing
    protected abstract boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
