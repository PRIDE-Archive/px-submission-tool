package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.model.MsRun;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 10:30
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public abstract class MzTabMsRunLineItemParsingHandler extends MetaDataLineItemParsingHandler {
    private static final Logger logger = LoggerFactory.getLogger(MzTabMsRunLineItemParsingHandler.class);

    protected static final String MZTAB_MSRUN_ITEM_PREFIX = "ms_run";
    private int index = 0;
    private String subItemString = "";
    private String subItemValueString = "";

    protected int getIndex() {
        return index;
    }

    private void setIndex(int index) {
        this.index = index;
    }

    protected String getSubItemString() {
        return subItemString;
    }

    private void setSubItemString(String subItemString) {
        this.subItemString = subItemString;
    }

    protected String getSubItemValueString() {
        return subItemValueString;
    }

    private void setSubItemValueString(String subItemValueString) {
        this.subItemValueString = subItemValueString;
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
        String[] lineItems = line.split("\t");
        if (lineItems.length == 3) {
            // Extract item index
            setIndex(Integer.valueOf(lineItems[1].substring(lineItems[1].indexOf('[') + 1, lineItems[0].indexOf(']'))));
            setSubItemString(lineItems[1].substring(lineItems[1].indexOf(']') + 1).trim());
            setSubItemValueString(lineItems[1]);
            return processEntry(context, lineNumber, offset);
        }
        return false;
    }

    // Delegate processing
    protected abstract boolean processEntry(MzTabParser context, long lineNumber, long offset) throws LineItemParsingHandlerException;
}
