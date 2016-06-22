package uk.ac.ebi.pride.gui.data.mztab.parser;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 16:50
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class DummyIndexedLineItemWithPropertyBeanBean implements MetaDataLineItemParsingHandler.IndexedLineItemWithPropertyBean {
    private String lineItemKey = null;
    private int index = -1;
    private String propertyKey = null;
    private String propertyValue = null;

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
    public void setPropertyKey(String pk) {
        propertyKey = pk;
    }

    @Override
    public String getPropertyValue() {
        return propertyValue;
    }

    @Override
    public void setPropertyValue(String pv) {
        propertyValue = pv;
    }
}
