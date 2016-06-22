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
 *
 *
 */

public class MetadataIndexedItemParserStrategyTest {

    @Test
    public void falseOnEmptyLine() {
        assertThat("False on empty line", MetadataIndexedItemParserStrategy.parseLine(new DummyIndexedItemWithPropertyBean(), ""), is(false));
    }

    @Test(expected = IndexedItemWithPropertyParserException.class)
    public void cantParseMalformedLines() {
        assertThat("False for not mzTab line", MetadataIndexedItemParserStrategy.parseLine(new DummyIndexedItemWithPropertyBean(), "this is not valid"), is(false));
        assertThat("False for all malformed mzTab line, when only line start is present",
                MetadataIndexedItemParserStrategy.parseLine(new DummyIndexedItemWithPropertyBean(), "MTD\t"), is(false));
        assertThat("False for all malformed mzTab line, when missing property, index and value",
                MetadataIndexedItemParserStrategy.parseLine(new DummyIndexedItemWithPropertyBean(), "MTD\ttestKey"), is(false));
        assertThat("False for all malformed mzTab line, when missing property key and value",
                MetadataIndexedItemParserStrategy.parseLine(new DummyIndexedItemWithPropertyBean(), "MTD\ttestKey[12]"), is(false));
        assertThat("False for all malformed mzTab line, when missing property value",
                MetadataIndexedItemParserStrategy.parseLine(new DummyIndexedItemWithPropertyBean(), "MTD\ttestKey[12]-testPropertyKey"), is(false));
        // Exception for NAN indexes
        MetadataIndexedItemParserStrategy.parseLine(new DummyIndexedItemWithPropertyBean(), "MTD\ttestKey[I am not a number]-testPropertyKey\ttest property value");
    }
}