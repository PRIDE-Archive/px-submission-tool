package uk.ac.ebi.pride.gui.data.mztab.parser;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.ebi.pride.gui.data.mztab.parser.exceptions.LineItemParsingHandlerException;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-24 11:47
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
@RunWith(Parameterized.class)
@NotThreadSafe
public class MetadataIndexedLineItemWithIndexedPropertyProcessingStrategiesParsingStackTest {
    // Bean
    private String lineStart;
    private String lineItemKey;
    private int index;
    private String propertyKey;
    private String propertyValue;
    private int propertyEntryIndex;
    // Test cases data
    private String testSubjectDescription;
    private LineItemParsingHandler subject;
    private DummyMzTabParser context;

    private static String getMzTabLine(String section, String item, int itemIndex, String attribute, String attributeValue) {
        return section + "\t" + item + "[" + itemIndex + "]-" + attribute + "\t" + attributeValue;
    }

    private static String getMzTabLine(String section, String item, int itemIndex, String attribute, int attributeIndex, String attributeValue) {
        return section + "\t" + item + "[" + itemIndex + "]-" + attribute + "[" + attributeIndex + "]" + "\t" + attributeValue;
    }

    public MetadataIndexedLineItemWithIndexedPropertyProcessingStrategiesParsingStackTest(String lineStart,
                                                                                          String lineItemKey,
                                                                                          int index,
                                                                                          String propertyKey,
                                                                                          int propertyEntryIndex,
                                                                                          String propertyValue,
                                                                                          String testSubjectDescription,
                                                                                          LineItemParsingHandler subject) {
        this.lineStart = lineStart;
        this.lineItemKey = lineItemKey;
        this.index = index;
        this.propertyKey = propertyKey;
        this.propertyValue = propertyValue;
        this.propertyEntryIndex = propertyEntryIndex;
        this.testSubjectDescription = testSubjectDescription;
        this.subject = subject;
        this.context = null;
    }

    @Before
    public void prepareEmptyContext() {
        this.context = new DummyMzTabParser("dummyfile");
    }

    // Test for manipulated line item key
    @Test
    public void rejectIncorrectLineItemKey() {
        assertThat("mzTab line rejected because of line item key mismatch, by " + testSubjectDescription,
                subject.parseLine(context, getMzTabLine(lineStart, "ñlkajsdflkj", index, propertyKey, propertyEntryIndex, propertyValue), 1, 0),
                is(false));
    }

    // Test for manipulated line item property key
    @Test
    public void rejectIncorrectPropertyKey() {
        assertThat("mzTab line rejected because of incorrect property key, by " + testSubjectDescription,
                subject.parseLine(context, getMzTabLine(lineStart, lineItemKey, index, "lkasjdf", propertyEntryIndex, propertyValue), 1, 0),
                is(false));
    }

    // Test for missing index
    @Test(expected = LineItemParsingHandlerException.class)
    public void rejectLineWithMissingIndex() {
        String mzTabLine = getMzTabLine(lineStart, lineItemKey, index, propertyKey, propertyEntryIndex, propertyValue);
        mzTabLine = mzTabLine.replaceFirst("\\[\\d+\\]", "[]");
        subject.parseLine(context, mzTabLine, 1, 0);
    }

    // Test for missing property entry index
    @Test(expected = LineItemParsingHandlerException.class)
    public void rejectLineWithMissingPropertyKeyIndex() {
        String mzTabLine = getMzTabLine(lineStart, lineItemKey, index, propertyKey, propertyEntryIndex, propertyValue);
        String effectiveMztabLine = mzTabLine.substring(0, mzTabLine.indexOf(']') + 1)
                + mzTabLine.substring(mzTabLine.indexOf(']') + 1).replaceFirst("\\[\\d+\\]", "[]");
        subject.parseLine(context, effectiveMztabLine, 1, 0);
    }

    // Test for bad index formatting
    @Test(expected = LineItemParsingHandlerException.class)
    public void rejectLineWithBadIndexFormatting() {
        String mzTabLine = getMzTabLine(lineStart, lineItemKey, index, propertyKey, propertyEntryIndex, propertyValue);
        mzTabLine = mzTabLine.replaceFirst("\\[\\d+\\]", "[34.98]");
        subject.parseLine(context, mzTabLine, 1, 0);
    }

    // Test for bad property index formatting
    @Test(expected = LineItemParsingHandlerException.class)
    public void rejectLineWithBadPropertyEntryIndexFormat() {
        String mzTabLine = getMzTabLine(lineStart, lineItemKey, index, propertyKey, propertyEntryIndex, propertyValue);
        String effectiveMztabLine = mzTabLine.substring(0, mzTabLine.indexOf(']') + 1)
                + mzTabLine.substring(mzTabLine.indexOf(']') + 1).replaceFirst("\\[\\d+\\]", "[34.98]");
        subject.parseLine(context, effectiveMztabLine, 1, 0);
    }
    // Test for NAN index
    @Test(expected = LineItemParsingHandlerException.class)
    public void rejectLineWithNanIndex() {
        String mzTabLine = getMzTabLine(lineStart, lineItemKey, index, propertyKey, propertyEntryIndex, propertyValue);
        mzTabLine = mzTabLine.replaceFirst("\\[\\d+\\]", "[Im_not_a_number]");
        subject.parseLine(context, mzTabLine, 1, 0);
    }

    // Test for NAN property entry index
    @Test(expected = LineItemParsingHandlerException.class)
    public void rejectLineWithNanPropertyEntryIndex() {
        String mzTabLine = getMzTabLine(lineStart, lineItemKey, index, propertyKey, propertyEntryIndex, propertyValue);
        String effectiveMztabLine = mzTabLine.substring(0, mzTabLine.indexOf(']') + 1)
                + mzTabLine.substring(mzTabLine.indexOf(']') + 1).replaceFirst("\\[\\d+\\]", "[Im_not_a_number]");
        subject.parseLine(context, effectiveMztabLine, 1, 0);
    }

    // Test for duplicated entry
    /*@Test(expected = LineItemParsingHandlerException.class)
    public void rejectDuplicatedEntry() {
        String mzTabLine = getMzTabLine(lineStart, lineItemKey, index, propertyKey, propertyValue);
        subject.parseLine(context, mzTabLine, 1, 0);
        // Inserting the same line for the second time, triggers the error
        subject.parseLine(context, mzTabLine, 1, 0);
    }*/

    // Everything ok test
    @Test
    public void successfulParsing() {
        String mzTabLine = getMzTabLine(lineStart, lineItemKey, index, propertyKey, propertyEntryIndex, propertyValue);
        assertThat("mzTab line is successfully parsed by " + testSubjectDescription,
                subject.parseLine(context, mzTabLine, 1, 0), is(true));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> subjectsToTest() {
        return Arrays.asList(new Object[][]{
                {"MTD", "sample", 90, "species", 2, "[NEWT, 3702, Arabidopsis thaliana (Mouse-ear cress), ]", "sample species parser", new QuickMzTabSampleSpeciesLineItemParsingHandler()}
        });
    }
}
