package uk.ac.ebi.pride.gui.data.mztab.parser;

import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.MetadataIndexedItemParserStrategyException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 14:26
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * Collection of parsing strategies for several types of indexed entries in the metadata section, e.g.
 * <line_start>\t<lineItemKey>[index]-<propertyKey>\t<propertyValue>
 *
 * It captures the data into the given bean
 */

public abstract class MetadataIndexedItemParserStrategy {

    private static boolean getLineItemKey(MetaDataLineItemParsingHandler.LineItemBean bean, String[] lineItems) throws MetadataIndexedItemParserStrategyException {
        bean.setLineItemKey(lineItems[1].substring(0, lineItems[1].indexOf('[')));
        return true;
    }

    private static boolean getLineItemIndex(MetaDataLineItemParsingHandler.IndexedLineItemBean bean, String[] lineItems) throws MetadataIndexedItemParserStrategyException {
        bean.setIndex(Integer.valueOf(lineItems[1].substring(lineItems[1].indexOf('[') + 1, lineItems[1].indexOf(']'))));
        return true;
    }

    private static boolean getPropertyKeyIfExists(MetaDataLineItemParsingHandler.IndexedLineItemWithPropertyBean bean, String[] lineItems) throws MetadataIndexedItemParserStrategyException {
        try {
            bean.setPropertyKey(lineItems[1].substring(lineItems[1].indexOf(']') + 2).trim());
        } catch (StringIndexOutOfBoundsException e) {
            // There is no property key, should we keep it absent?
            //bean.setPropertyKey("");
            return false;
        }
        return true;
    }

    private static boolean getPropertyValue(MetaDataLineItemParsingHandler.IndexedLineItemWithPropertyBean bean, String[] lineItems) throws MetadataIndexedItemParserStrategyException {
        bean.setPropertyValue(lineItems[2].trim());
        return true;
    }

    private static boolean getPropertyEntryIndexIfExists(MetaDataLineItemParsingHandler.IndexedLineItemWithIndexedPropoertyDataEntry bean, String[] lineItems) throws MetadataIndexedItemParserStrategyException {
        // We need to get the second index in the entry
        try {
            String afterFirstSquareBracket = lineItems[1].substring(lineItems[1].indexOf(']') + 1);
            bean.setPropertyEntryIndex(Integer.valueOf(afterFirstSquareBracket.substring(afterFirstSquareBracket.indexOf('[') + 1, afterFirstSquareBracket.indexOf(']'))));
        } catch (Exception e) {
            // Any thing that could happen here means we were not able to parse the thing
            return false;
        }
        return true;
    }

    // Parse indexed items with properties
    public static boolean parseLine(MetaDataLineItemParsingHandler.IndexedLineItemWithPropertyBean bean, String line) throws MetadataIndexedItemParserStrategyException {
        String[] lineItems = line.split("\t");
        try {
            if (lineItems.length == 3) {
                // Extract data
                return getLineItemKey(bean, lineItems)
                        && getLineItemIndex(bean, lineItems)
                        && getPropertyKeyIfExists(bean, lineItems)
                        && getPropertyValue(bean, lineItems);
            }
        } catch (Exception e) {
            throw new MetadataIndexedItemParserStrategyException(e.getMessage());
        }
        return false;
    }

    public static boolean parseLine(MetaDataLineItemParsingHandler.IndexedLineItemWithIndexedPropoertyDataEntry bean, String line) throws MetadataIndexedItemParserStrategyException {
        String[] lineItems = line.split("\t");
        try {
            if (lineItems.length == 3) {
                // Get the data
                return getLineItemKey(bean, lineItems)
                        && getLineItemIndex(bean, lineItems)
                        && getPropertyKeyIfExists(bean, lineItems)
                        && getPropertyValue(bean, lineItems)
                        && getPropertyEntryIndexIfExists(bean, lineItems);
            }
        } catch (Exception e) {
            throw new MetadataIndexedItemParserStrategyException(e.getMessage());
        }
        return false;
    }
}
