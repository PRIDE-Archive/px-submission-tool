package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.Software;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.MetadataLineItemParserStrategyException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-07 16:29
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class MzTabSoftwareLineItemParsingHandler extends MetaDataLineItemParsingHandler implements MetaDataLineItemParsingHandler.IndexedLineItemWithIndexedPropertyDataEntry {
    protected static final String MZTAB_SOFTWARE_ITEM_KEY = "software";

    // Bean Defaults
    // TODO - Refactor out this bean
    protected static final String DEFAULT_LINE_ITEM_KEY = "";
    protected static final int DEFAULT_INDEX = -1;
    protected static final String DEFAULT_PROPERTY_KEY = "";
    protected static final String DEFAULT_PROPERTY_VALUE = "";
    protected static final int DEFAULT_PROPERTY_ENTRY_INDEX = -1;
    // Bean attributes
    private String lineItemKey = DEFAULT_LINE_ITEM_KEY;
    private int index = DEFAULT_INDEX;
    private String propertyKey = DEFAULT_PROPERTY_KEY;
    private String propertyValue = DEFAULT_PROPERTY_VALUE;
    private int propertyEntryIndex = DEFAULT_PROPERTY_ENTRY_INDEX;

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

    @Override
    public int getPropertyEntryIndex() {
        return propertyEntryIndex;
    }

    @Override
    public void setPropertyEntryIndex(int propertyEntryIndex) {
        this.propertyEntryIndex = propertyEntryIndex;
    }

    private void cleanBean() {
        lineItemKey = DEFAULT_LINE_ITEM_KEY;
        index = DEFAULT_INDEX;
        propertyKey = DEFAULT_PROPERTY_KEY;
        propertyValue = DEFAULT_PROPERTY_VALUE;
        propertyEntryIndex = DEFAULT_PROPERTY_ENTRY_INDEX;
    }

    protected Software getSoftwareEntryFromContext(MzTabParser context, int softwareEntryIndex) {
        Software software = context.getMetaDataSection().getSoftware(softwareEntryIndex);
        if (software == null) {
            software = new Software();
            context.getMetaDataSection().updateSoftware(software, softwareEntryIndex);
        }
        return software;
    }

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        // TODO - Consider refactoring this algorithm out
        cleanBean();
        try {
            if (MetadataLineItemParserStrategy.parseLine(this, line)) {
                if (getIndex() == DEFAULT_INDEX) {
                    throw new LineItemParsingHandlerException("MISSING INDEX for '" + MZTAB_SOFTWARE_ITEM_KEY + "' at line '" + lineNumber + "'");
                }
                return processEntry(context, lineNumber, offset);
            }
        } catch (MetadataLineItemParserStrategyException e) {
            throw new LineItemParsingHandlerException(e.getMessage());
        }
        return false;
    }

    // Delegate processing
    protected abstract boolean processEntry(MzTabParser context, long lineNumber, long offset);
}
