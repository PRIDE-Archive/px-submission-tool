package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 16:41
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
@RunWith(Parameterized.class)
public class MetadataIndexedItemParserStrategyBulkTest {
    private String lineStart;
    private String lineItemKey;
    private int index;
    private String propertyKey;
    private String propertyValue;
    private String testDescription;

    private static String getMzTabLine(String start, String lineItemKey, int index, String propertyKey, String propertyValue) {
        return start + "\t" + lineItemKey + "[" + index + "]-" + propertyKey + "\t" + propertyValue;
    }

    public MetadataIndexedItemParserStrategyBulkTest(String lineStart, String lineItemKey, int index, String propertyKey, String propertyValue, String testDescription) {
        this.lineStart = lineStart;
        this.lineItemKey = lineItemKey;
        this.index = index;
        this.propertyKey = propertyKey;
        this.propertyValue = propertyValue;
        this.testDescription = testDescription;
    }

    @Test
    public void checkThatParseDataMatchesOriginal() {
        DummyIndexedLineItemWithPropertyBean bean = new DummyIndexedLineItemWithPropertyBean();
        assertThat("mzTab line with missing fields passes",
                MetadataIndexedItemParserStrategy.parseLine(bean, getMzTabLine(lineStart, lineItemKey, index, propertyKey, propertyValue)),
                is(true));
        assertThat(testDescription, bean.getLineItemKey(), equalToIgnoringCase(lineItemKey));
        assertThat(testDescription, bean.getIndex() == index, is(true));
        assertThat(testDescription, bean.getPropertyKey(), equalToIgnoringCase(propertyKey));
        assertThat(testDescription, bean.getPropertyValue(), equalToIgnoringCase(propertyValue));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // Empty value at the end is a special case, that has to do with trailing new line characters or not,
                // as a general rule, we'll consider missing values as missing the entire last part, thus, not parsing
                // when a particular number of items is involved
                //{"MTD", "ms-run", 0, "format", "", "Empty Value"},
                {"MTD", "ms-run", 1, "format", "[MS, MS:1000584, mzML file, ]", "Valid format value"},
                {"MTD", "ms-run", 2, "id_format", "[MS, MS:1000530, mzML unique identifier, ]", "Valid id_format value"},
                {"MTD", "ms-run", 3, "location", "file://platelets_lysate-24.mzML", "Location data"},
                {"MTD", "ms-run", 4, "", "sample test value", "Property key is empty"}
        });
    }

}