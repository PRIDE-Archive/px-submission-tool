package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.MetadataIndexedItemParserStrategyException;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 14:26
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 * <p>
 * Collection of parsing strategies for several types of indexed entries in the metadata section, e.g.
 * <line_start>\t<lineItemKey>[index]-<propertyKey>\t<propertyValue>
 * <p>
 * It captures the data into the given bean
 */

public abstract class MetadataIndexedItemParserStrategy {
    private static final Logger logger = LoggerFactory.getLogger(MetadataIndexedItemParserStrategy.class);

    private static int getStrictIndex(String s) throws MetadataIndexedItemParserStrategyException {
        int index = -1;
        if (!s.isEmpty()) {
            try {
                index = Integer.valueOf(s);
            } catch (NumberFormatException e) {
                throw new MetadataIndexedItemParserStrategyException(e.getMessage());
            }
        } else {
            throw new MetadataIndexedItemParserStrategyException("CANNOT PARSE INDEX out of EMPTY STRING");
        }
        return index;
    }

    private static boolean getLineItemKey(MetaDataLineItemParsingHandler.LineItemBean bean, String[] lineItems) throws MetadataIndexedItemParserStrategyException {
        try {
            String lineItemKey = lineItems[1].substring(0, lineItems[1].indexOf('['));
            logger.debug("Parsed line item key '" + lineItemKey + "'");
            bean.setLineItemKey(lineItemKey);
        } catch (IndexOutOfBoundsException e) {
            throw new MetadataIndexedItemParserStrategyException(e.getMessage());
        }
        return true;
    }

    private static boolean getLineItemIndex(MetaDataLineItemParsingHandler.IndexedLineItemBean bean, String[] lineItems) throws MetadataIndexedItemParserStrategyException {
        String integerString = null;
        try {
            integerString = lineItems[1].substring(lineItems[1].indexOf('[') + 1, lineItems[1].indexOf(']'));
        } catch (IndexOutOfBoundsException e) {
            throw new MetadataIndexedItemParserStrategyException(e.getMessage());
        }
        logger.debug("Reading line item index '" + integerString + "'");
        int index = getStrictIndex(integerString);
        // Check that it is possitive
        if (index < 0) {
            throw new MetadataIndexedItemParserStrategyException("INVALID NEGATIVE line item index");
        }
        bean.setIndex(index);
        return true;
    }

    private static boolean getPropertyKeyIfExists(MetaDataLineItemParsingHandler.IndexedLineItemWithPropertyBean bean, String[] lineItems) throws MetadataIndexedItemParserStrategyException {
        try {
            String propertyKey = lineItems[1].substring(lineItems[1].indexOf(']') + 2).trim();
            if (propertyKey.indexOf('[') != -1) {
                // Refine it
                propertyKey = propertyKey.substring(0, propertyKey.indexOf('['));
            }
            logger.debug("Parsed property key '" + propertyKey + "'");
            bean.setPropertyKey(propertyKey);
        } catch (IndexOutOfBoundsException e) {
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

    private static boolean getPropertyEntryIndexIfExists(MetaDataLineItemParsingHandler.IndexedLineItemWithIndexedPropertyDataEntry bean, String[] lineItems) throws MetadataIndexedItemParserStrategyException {
        // We need to get the second index in the entry
        try {
            String afterFirstSquareBracket = lineItems[1].substring(lineItems[1].indexOf(']') + 1);
            if (afterFirstSquareBracket.indexOf('[') != -1) {
                String indexStringToParse = afterFirstSquareBracket.substring(afterFirstSquareBracket.indexOf('[') + 1, afterFirstSquareBracket.indexOf(']'));
                logger.debug("Processing property index '" + indexStringToParse + "'");
                int index = getStrictIndex(indexStringToParse);
                if (index < 0) {
                    throw new MetadataIndexedItemParserStrategyException("INVALID NEGATIVE property entry index");
                }
                bean.setPropertyEntryIndex(index);
                return true;
            }
        } catch (IndexOutOfBoundsException e) {
            // No property index
        }
        return false;
    }

    // Parse indexed items without any other properties
    public static boolean parseLine(MetaDataLineItemParsingHandler.IndexedLineItemBean bean, String line) throws MetadataIndexedItemParserStrategyException {
        String[] lineItems = line.split("\t");
        try {
            if (lineItems.length == 3) {
                // Extract data
                return getLineItemKey(bean, lineItems)
                        && getLineItemIndex(bean, lineItems);
            }
        } catch (Exception e) {
            throw new MetadataIndexedItemParserStrategyException(e.getMessage());
        }
        return false;
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

    public static boolean parseLine(MetaDataLineItemParsingHandler.IndexedLineItemWithIndexedPropertyDataEntry bean, String line) throws MetadataIndexedItemParserStrategyException {
        String[] lineItems = line.split("\t");
        logger.debug(">>> >>> >>> PARSING indexed line item with indexed property <<< <<< <<<");
        try {
            if (lineItems.length == 3) {
                // Get the data
                // Property key is optional
                getPropertyKeyIfExists(bean, lineItems);
                // That property to be indexed is also optional
                getPropertyEntryIndexIfExists(bean, lineItems);
                // This way, we can reuse this algorithm for elements that are like fixed_mod[index],
                // fixed_mod[index]-propertyKey, sample[index]-description, sample[index]-species[property_key_index]
                // TODO - I could probably refactor this later, to merge several type of line item attributes
                return getLineItemKey(bean, lineItems)
                        && getLineItemIndex(bean, lineItems)
                        && getPropertyValue(bean, lineItems);
            }
        } catch (Exception e) {
            throw new MetadataIndexedItemParserStrategyException(e.getMessage());
        }
        return false;
    }
}
