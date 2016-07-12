package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.VariableMod;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.MetadataLineItemParserStrategyException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 10:57
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class MzTabVariableModLineItemParsingHandler extends MetaDataLineItemParsingHandler implements MetaDataLineItemParsingHandler.IndexedLineItemWithPropertyBean {
    private static final Logger logger = LoggerFactory.getLogger(MzTabVariableModLineItemParsingHandler.class);

    protected static final String MZTAB_VARIABLE_MOD_ITEM_KEY = "variable_mod";
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

    protected VariableMod getVariableModFromContext(MzTabParser context, int variableModIndex) {
        VariableMod variableMod = context.getMetaDataSection().getVariableMod(variableModIndex);
        if (variableMod == null) {
            variableMod = new VariableMod();
            context.getMetaDataSection().updateVariableMod(variableMod, variableModIndex);
        }
        return variableMod;
    }

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        // TODO - Consider refactoring this algorithm out
        try {
            if (MetadataLineItemParserStrategy.parseLine(this, line)) {
                if (getLineItemKey().equals(MZTAB_VARIABLE_MOD_ITEM_KEY)) {
                    if (getIndex() == DEFAULT_INDEX) {
                        throw new LineItemParsingHandlerException("MISSING INDEX for '" + MZTAB_VARIABLE_MOD_ITEM_KEY + "'");
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
