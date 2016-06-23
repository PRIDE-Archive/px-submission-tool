package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.model.Sample;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.MetadataIndexedItemParserStrategyException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-22 13:26
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public abstract class MzTabSampleLineItemParsingHandler extends MetaDataLineItemParsingHandler implements MetaDataLineItemParsingHandler.IndexedLineItemWithIndexedPropertyDataEntry {

    protected static final String MZTAB_SAMPLE_ITEM_PREFIX = "sample";
    private String lineItemKey = "";
    private int index = 0;
    private String propertyKey = "";
    private String propertyValue = "";
    private int propertyEntryIndex = -1;

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

    protected Sample getSampleFromContext(MzTabParser context, int sampleIndex) {
        Sample sample = context.getMetaDataSection().getSampleData(sampleIndex);
        if (sample == null) {
            sample = new Sample();
            context.getMetaDataSection().updateSampleData(sample, sampleIndex);
        }
        return sample;
    }

    protected Sample.DataEntry getSampleDataEntryFromContext(MzTabParser context, int sampleIndex, int dataEntryIndex) {
        Sample.DataEntry dataEntry = getSampleFromContext(context, sampleIndex).getDataEntry(dataEntryIndex);
        if (dataEntry == null) {
            dataEntry = new Sample.DataEntry();
            getSampleFromContext(context, sampleIndex).updateDataEntry(dataEntry, dataEntryIndex);
        }
        return dataEntry;
    }

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset)
            throws LineItemParsingHandlerException {
        // TODO - I should probably refactor this code out to a superclass for all those subclasses dealing with indexed
        // TODO - line items, with or without properties share the same code
        try {
            if (MetadataIndexedItemParserStrategy.parseLine(this, line)) {
                if (getLineItemKey().equals(MZTAB_SAMPLE_ITEM_PREFIX)) {
                    // The line item key is ok, go ahead
                    return processEntry(context, lineNumber, offset);
                }
            }
        } catch (MetadataIndexedItemParserStrategyException e) {
            throw new LineItemParsingHandlerException(e.getMessage());
        }
        return false;
    }

    // Delegate processing
    protected abstract boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
