package uk.ac.ebi.pride.gui.data.mztab.parser;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-11 16:04
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
public class TestsLittleHelper {

    public static final String getMzTabLineForIndexedLineItemWithProperty(String section, String item, int itemIndex, String attribute, String attributeValue) {
        return section + "\t" + item + "[" + itemIndex + "]-" + attribute + "\t" + attributeValue;
    }

    public static final String getMzTabLineForLineItem(String section, String item, String attributeValue) {
        return section + "\t" + item + "\t" + attributeValue;
    }
}
