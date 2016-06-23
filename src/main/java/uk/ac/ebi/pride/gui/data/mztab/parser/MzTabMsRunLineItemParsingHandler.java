package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.MsRun;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.MetadataIndexedItemParserStrategyException;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 10:30
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * Root of the hierarchy of classes that will process ms-run entries in the mzTab file
 *
 * It does the first simple parsing of the ms-run entry, checking it at line item key level, i.e. it checks that the
 * given line corresponds to an ms-run entry, and delegates identification and further processing of its particular data
 * (location, format, id_format...)
 */

public abstract class MzTabMsRunLineItemParsingHandler extends MetaDataLineItemParsingHandler implements MetaDataLineItemParsingHandler.IndexedLineItemWithPropertyBean {
    private static final Logger logger = LoggerFactory.getLogger(MzTabMsRunLineItemParsingHandler.class);

    protected static final String MZTAB_MSRUN_ITEM_PREFIX = "ms_run";
    // Bean Defaults
    private static final String DEFAULT_LINE_ITEM_KEY = "";
    private static final int DEFAULT_INDEX = -1;
    private static final String DEFAULT_PROPERTY_KEY = "";
    private static final String DEFAULT_PROPERTY_VALUE = "";
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
        logger.debug("Cleaning Bean data");
        lineItemKey = DEFAULT_LINE_ITEM_KEY;
        index = DEFAULT_INDEX;
        propertyKey = DEFAULT_PROPERTY_KEY;
        propertyValue = DEFAULT_PROPERTY_VALUE;
    }

    // Working with MsRun objects from context

    /**
     * Access the indexed MS-Run entry, given an index.
     * If there is no MsRun object set in the context for that index, it will create an empty one, this makes sense because
     * we are accessing MsRun in the context of a parser, that is extracting information from a data file
     * @param context parser context
     * @param msRunIndex index of the entry we want to retrieve
     * @return the MsRun entry of null if there is no entry associated to that index
     */
    protected MsRun getMsRunFromContext(MzTabParser context, int msRunIndex) {
        MsRun msRun = context.getMetaDataSection().getMsRunEntry(msRunIndex);
        logger.debug("Existing MsRun entry for index " + msRunIndex + " is " + msRun);
        if (msRun == null) {
            logger.debug("MsRun entry for index " + msRunIndex + " not present, creating a new one");
            msRun = new MsRun();
            context.getMetaDataSection().updateMsRun(msRun, msRunIndex);
        }
        return msRun;
    }

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        // Clear the bean
        cleanBean();
        logger.debug(">>> PARSING LINE ITEM: " + line);
        try {
            if (MetadataIndexedItemParserStrategy.parseLine(this, line)) {
                if (getLineItemKey().equals(MZTAB_MSRUN_ITEM_PREFIX)) {
                    // The line item key is ok, go ahead
                    //TODO - REMOVE - logger.debug("Line item key is '" + getLineItemKey() + "' AND this parser is looking for '" + MZTAB_MSRUN_ITEM_PREFIX + "', GO AHEAD");
                    return processEntry(context, lineNumber, offset);
                }
                //TODO - REMOVE - logger.debug("Line item key is '" + getLineItemKey() + "' but this parser is looking for '" + MZTAB_MSRUN_ITEM_PREFIX + "'");
                //TODO - REMOVE - return false;
            }
        } catch (MetadataIndexedItemParserStrategyException e) {
            throw new LineItemParsingHandlerException(e.getMessage());
        }
        return false;
    }

    // Delegate processing
    protected abstract boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
