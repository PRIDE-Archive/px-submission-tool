package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.apache.http.annotation.NotThreadSafe;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-07-12 9:08
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */
@RunWith(Parameterized.class)
@NotThreadSafe
public class MetadataLineItemProcessingStrategiesParsingStackTest {
    private String lineStart;
    private String lineItemKey;
    private String propertyValue;
    private String testDescription;
    private LineItemParsingHandler subject;
    private DummyMzTabParser context;

    public MetadataLineItemProcessingStrategiesParsingStackTest(String lineStart,
                                                                String lineItemKey,
                                                                String propertyValue,
                                                                String testDescription,
                                                                LineItemParsingHandler subject) {
        this.lineStart = lineStart;
        this.lineItemKey = lineItemKey;
        this.propertyValue = propertyValue;
        this.testDescription = testDescription;
        this.subject = subject;
    }

    // TODO - I know there are some duplicated tests here compared to
    // TODO - MetadataIndexedLineItemProcessingStrategiesParsingStackTest, I may refactor this later down the road
    @Before
    public void prepareEmptyContext() {
        this.context = new DummyMzTabParser("dummyfile");
    }

    // Test for manipulated line item key
    @Test
    @Ignore
    public void rejectIncorrectLineItemKey() {
        assertThat("mzTab line rejected because of line item key mismatch, by " + testDescription,
                subject.parseLine(context, TestsLittleHelper.getMzTabLineForLineItem(lineStart, "ñlkajsdflkj", propertyValue), 1, 0),
                is(false));
    }

    // Everything ok test
    @Test
    public void successfulParsing() {
        String mzTabLine = TestsLittleHelper.getMzTabLineForLineItem(lineStart, lineItemKey, propertyValue);
        assertThat("mzTab line parses successfuly for " + this.testDescription,
                subject.parseLine(context, mzTabLine, 1, 0), is(true));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> subjectsToTest() {
        return Arrays.asList(new Object[][]{
                {"MTD", "quantification_method", "[MS, MS:1001837, iTRAQ quantitation analysis, ]", "Quantification method", new QuickMzTabQuantificationMethodLineItemParsingHandler()},
                {"MTD", "protein_quantification_unit", "[PRIDE, PRIDE:0000395, Ratio, ]", "Protein quantification unit", new QuickMzTabProteinQuantificationUnitLineItemParsingHandler()},
                {"MTD", "peptide_quantification_unit", "[PRIDE, PRIDE:0000395, Ratio, ]", "Peptide quantification unit", new QuickMzTabPeptideQuantificationUnitLineItemParsingHandler()},
                {"MTD", "small_molecule-quantification_unit", "[PRIDE, PRIDE:0000395, Ratio, ]", "Small Molecule quantification unit", new QuickMzTabSmallMoleculeQuantificationUnitLineItemParsingHandler()}
        });
    }
}
