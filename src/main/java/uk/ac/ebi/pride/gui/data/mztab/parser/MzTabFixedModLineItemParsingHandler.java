package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.FixedMod;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.MetadataLineItemParserStrategyException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-06 16:25
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class MzTabFixedModLineItemParsingHandler extends MetaDataLineItemParsingHandler implements MetaDataLineItemParsingHandler.IndexedLineItemWithPropertyBean {
    private static final Logger logger = LoggerFactory.getLogger(MzTabFixedModLineItemParsingHandler.class);

    protected static final String MZTAB_FIXED_MOD_ITEM_KEY = "fixed_mod";
    // Bean Defaults
    // TODO - Refactor out the bean part
    protected static final String DEFAULT_LINE_ITEM_KEY = "";
    protected static final int DEFAULT_INDEX = -1;
    protected static final String DEFAULT_PROPERTY_KEY = "";
    protected static final String DEFAULT_PROPERTY_VALUE = "";
    // Bean attributes
    private String lineItemKey = DEFAULT_LINE_ITEM_KEY;
    private int index = DEFAULT_INDEX;
    private String propertyKey = DEFAULT_PROPERTY_KEY;
    private String propertyValue = DEFAULT_PROPERTY_VALUE;

    @Override
    public String getLineItemKey() {
        return lineItemKey;
    }

    @Override
    public void setLineItemKey(String lineItemKey) {
        this.lineItemKey = lineItemKey;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String getPropertyKey() {
        return propertyKey;
    }

    @Override
    public void setPropertyKey(String propertyKey) {
        this.propertyKey = propertyKey;
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
        propertyKey = DEFAULT_PROPERTY_KEY;
        propertyValue = DEFAULT_PROPERTY_VALUE;
    }

    protected FixedMod getFixedModFromContext(MzTabParser context, int fixedModIndex) {
        FixedMod fixedMod = context.getMetaDataSection().getFixedMod(fixedModIndex);
        if (fixedMod == null) {
            fixedMod = new FixedMod();
            context.getMetaDataSection().updateFixedMod(fixedMod, fixedModIndex);
        }
        return fixedMod;
    }

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        // TODO - Consider refactoring this algorithm out
        cleanBean();
        try {
            if (MetadataLineItemParserStrategy.parseLine(this, line)) {
                if (getLineItemKey().equals(MZTAB_FIXED_MOD_ITEM_KEY)) {
                    if (getIndex() == DEFAULT_INDEX) {
                        throw new LineItemParsingHandlerException("MISSING INDEX for '" + MZTAB_FIXED_MOD_ITEM_KEY + "' at line '" + lineNumber + "'");
                    }
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
