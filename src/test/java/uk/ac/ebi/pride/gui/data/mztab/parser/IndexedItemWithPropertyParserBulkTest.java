package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-21 16:41
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
@RunWith(Parameterized.class)
public class IndexedItemWithPropertyParserBulkTest {
    private String lineStart;
    private String lineItemKey;
    private int index;
    private String propertyKey;
    private String propertyValue;
    private String testDescription;

    private static String getMzTabLine(String start, String lineItemKey, int index, String propertyKey, String propertyValue) {
        return start + "\t" + lineItemKey + "[" + index + "]-" + propertyKey + "\t" + propertyValue;
    }

    public IndexedItemWithPropertyParserBulkTest(String lineStart, String lineItemKey, int index, String propertyKey, String propertyValue, String testDescription) {
        this.lineStart = lineStart;
        this.lineItemKey = lineItemKey;
        this.index = index;
        this.propertyKey = propertyKey;
        this.propertyValue = propertyValue;
        this.testDescription = testDescription;
    }

    @Test
    public void checkThatParseDataMatchesOriginal() {
        DummyIndexedItemWithPropertyBean bean = new DummyIndexedItemWithPropertyBean();
        IndexedItemWithPropertyParser.parseLine(bean, getMzTabLine(lineStart, lineItemKey, index, propertyKey, propertyValue));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"MTD", "ms-run", 0, "format", "", "Empty Value"},
                {"MTD", "ms-run", 1, "format", "[MS, MS:1000584, mzML file, ]", "Valid format value"},
                {"MTD", "ms-run", 2, "id_format", "[MS, MS:1000530, mzML unique identifier, ]", "Valid id_format value"},
                {"MTD", "ms-run", 3, "location", "file://platelets_lysate-24.mzML", "Location data"},
                {"MTD", "ms-run", 4, "", "sample test value", "Property key is empty"}
        });
    }

}