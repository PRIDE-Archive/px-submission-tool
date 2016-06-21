package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.MsRun;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.IndexedItemWithPropertyParserException;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 10:30
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public abstract class MzTabMsRunLineItemParsingHandler extends MetaDataLineItemParsingHandler implements MetaDataLineItemParsingHandler.IndexedItemWithProperty {
    private static final Logger logger = LoggerFactory.getLogger(MzTabMsRunLineItemParsingHandler.class);

    protected static final String MZTAB_MSRUN_ITEM_PREFIX = "ms_run";
    private int index = 0;
    private String propertyKey = "";
    private String propertyValue = "";

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
        if (msRun == null) {
            msRun = new MsRun();
            context.getMetaDataSection().updateMsRun(msRun, msRunIndex);
        }
        return msRun;
    }

    @Override
    protected boolean doParseLineItem(MzTabParser context, String line, long lineNumber, long offset) throws LineItemParsingHandlerException {
        try {
            if (IndexedItemWithPropertyParser.parseLine(this, line)) {
                return processEntry(context, lineNumber, offset);
            }
        } catch (IndexedItemWithPropertyParserException e) {
            throw new LineItemParsingHandlerException(e.getMessage());
        }
        return false;
    }

    // Delegate processing
    protected abstract boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
