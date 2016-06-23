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
    private String lineItemKey = "";
    private int index = -1;
    private String propertyKey = "";
    private String propertyValue = "";

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
        try {
            if (MetadataIndexedItemParserStrategy.parseLine(this, line)) {
                if (getLineItemKey().equals(MZTAB_MSRUN_ITEM_PREFIX)) {
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
