package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Test;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.IndexedItemWithPropertyParserException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 15:06
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class IndexedItemWithPropertyParserTest {
    private String lineStart = "MD";
    private String lineItemKey = "ms-run";
    private int index = 2;
    private String propertyKey = "format";
    private String propertyValue = "";

    // Different Values
    private String cvParamValue = "[PSI-MS, MS:1001062, Mascot MGF file, ]";
    private String urlValue = "file:Z:\\Proteomics\\CGehring\\ITurek\\CM all data\\My Experiment mzIdent export 14-Sep-2014 04-12-15-PM\\10pM Rep1\\20131201-pM-A49.mzid_20131201-pM-A49.MGF";
    private String emptyValue = "";

    public class DummyBean implements MetaDataLineItemParsingHandler.IndexedItemWithProperty {
        private int index;
        private String propertyKey;
        private String propertyValue;

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

    private MetaDataLineItemParsingHandler.IndexedItemWithProperty bean;

    private static String getMzTabLine(String start, String lineItemKey, int index, String propertyKey, String propertyValue) {
        return start + "\t" + lineItemKey + "[" + index + "]-" + propertyKey + "\t" + propertyValue;
    }

    @Test
    public void falseOnEmptyLine() {
        assertThat("False on empty line", IndexedItemWithPropertyParser.parseLine(new DummyBean(), ""), is(false));
    }

    @Test(expected = IndexedItemWithPropertyParserException.class)
    public void cantParseMalformedLines() {
        assertThat("False for not mzTab line", IndexedItemWithPropertyParser.parseLine(new DummyBean(), "this is not valid"), is(false));
        assertThat("False for all malformed mzTab line, when only line start is present",
                IndexedItemWithPropertyParser.parseLine(new DummyBean(), "MTD\t"), is(false));
        assertThat("False for all malformed mzTab line, when missing property, index and value",
                IndexedItemWithPropertyParser.parseLine(new DummyBean(), "MTD\ttestKey"), is(false));
        assertThat("False for all malformed mzTab line, when missing property key and value",
                IndexedItemWithPropertyParser.parseLine(new DummyBean(), "MTD\ttestKey[12]"), is(false));
        assertThat("False for all malformed mzTab line, when missing property value",
                IndexedItemWithPropertyParser.parseLine(new DummyBean(), "MTD\ttestKey[12]-testPropertyKey"), is(false));
        // Exception for NAN indexes
        IndexedItemWithPropertyParser.parseLine(new DummyBean(), "MTD\ttestKey[I am not a number]-testPropertyKey\ttest property value");
    }
}