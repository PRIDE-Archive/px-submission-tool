package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import uk.ac.ebi.pride.gui.data.mztab.model.MetaData;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-16 14:05
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

// NOTE: Up to this point of developing the product extension, I think some refactoring that moves the common tests to
// a test class that bulk-applies them to all the relevant strategies, would be a very nice code clean up task
public class QuickMzTabDescriptionLineItemHandlerTest {
    private static MzTabParser context;
    private LineItemParsingHandler subject;

    @BeforeClass
    public static void initDummies() {
        context = new DummyMzTabParser("dummyfile.txt");
    }

    @Before
    public void init() {
        subject = new QuickMzTabDescriptionLineItemHandler();
    }

    @Test
    public void returnFalseOnEmptyLine() {
        assertFalse("Return false when handling an empty line", subject.parseLine(context, "", 1, 0));
    }

    @Test
    public void returnFalseWhenInvalidKeyword() {
        assertFalse("Return false when invalid keyword", subject.parseLine(context, "MTD\tikjshdfksjhfd\tGarbage information", 1, 0));
    }

    @Test
    public void verifyVersionMatches() {
        MzTabParser context = Mockito.mock(DummyMzTabParser.class);
        MetaData metaData = Mockito.mock(MetaData.class);
        String description = "In plants, structural and physiological evidence has suggested the presence of " +
                "biologically active natriuretic peptides (PNPs). PNPs are secreted into the apoplast, are " +
                "systemically mobile and elicit a range of responses signaling via cGMP. The PNP-dependent " +
                "responses include tissue specific modifications of cation transport and changes in stomatal " +
                "conductance and the photosynthetic rate. PNP also has a critical role in host defense responses. " +
                "Surprisingly, PNP-homologues are also produced by several plant pathogens during host colonization " +
                "suppressing host defense responses. Here we show that a synthetic peptide representing the " +
                "biologically active fragment of the Arabidopsis thaliana PNP (AtPNP-A) induces the production of " +
                "reactive oxygen species in suspension-cultured A. thaliana (Col-0) cells.";
        String mzTabDescriptionLine = "MTD\tdescription\t" + description;
        when(context.getMetaDataSection()).thenReturn(metaData);
        subject.parseLine(context, mzTabDescriptionLine, 1, 0);
        verify(context, atLeastOnce()).getMetaDataSection();
        verify(metaData, times(1)).getDescription();
        verify(metaData, times(1)).setDescription(description);
    }


}