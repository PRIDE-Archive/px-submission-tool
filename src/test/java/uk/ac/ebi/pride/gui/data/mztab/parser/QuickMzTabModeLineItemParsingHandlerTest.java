package uk.ac.ebi.pride.gui.data.mztab.parser;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.ac.ebi.pride.gui.data.mztab.model.MetaData;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab.parser
 * Timestamp: 2016-06-15 11:47
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This is a collection of tests to run over a stand alone (last handler) QuickMzTabModeLineItemParsingHandler
 */

public class QuickMzTabModeLineItemParsingHandlerTest {
    private static MzTabParser context;
    private LineItemParsingHandler subject;

    @BeforeClass
    public static void initDummies() {
        context = new DummyMzTabParser("dummyfile.txt");
    }

    @Before
    public void init() {
        subject = new QuickMzTabModeLineItemParsingHandler();
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
    public void processCompleteMode() {
        // Although using a real parser and metadata is not expensive, I use mockito to avoid creating all the subproducts
        // needed to set up the context, and to focus on the tested subject's logic
        MzTabParser context = Mockito.mock(DummyMzTabParser.class);
        MetaData metaData = Mockito.mock(MetaData.class);
        String mztabCompleteModeLine = "MTD\tmzTab-mode\tComplete";
        when(context.getMetaDataSection()).thenReturn(metaData);
        subject.parseLine(context, mztabCompleteModeLine, 1, 0);
        verify(context, atLeastOnce()).getMetaDataSection();
        verify(metaData, times(1)).getMode();
        verify(metaData, times(1)).setMode(MetaData.MzTabMode.COMPLETE);
    }
}