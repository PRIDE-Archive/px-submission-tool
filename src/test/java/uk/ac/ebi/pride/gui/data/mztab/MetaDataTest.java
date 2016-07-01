package uk.ac.ebi.pride.gui.data.mztab;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.ac.ebi.pride.gui.data.mztab.model.*;

import java.net.MalformedURLException;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.data.mztab
 * Timestamp: 2016-06-08 09:54
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 */

public class MetaDataTest {

    // Testing getters setters is absurd

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void emptyMetadataDoesnotValidate() throws InvalidMzTabSectionException {
        MetaData metaData = new MetaData();
        assertThat("Empty metadata section does not validate", metaData.validate(new MzTabDocument(), new OneTimeDefaultValidatorMzTabSectionValidator()), is(false));
    }

    @Test
    @Ignore
    public void minimumInformationForValidation() throws MalformedURLException, InvalidMzTabSectionException {
        // TODO - This test may no longer work due to changes in the validation algorithm
        MetaData metaData = new MetaData();
        metaData.setTitle("Test Title");
        metaData.setVersion("1.0");
        metaData.setMode(MetaData.MzTabMode.COMPLETE);
        metaData.setType(MetaData.MzTabType.IDENTIFICATION);
        metaData.setDescription("Sample Description");
        MsRunFormat msRunFormat = new MsRunFormat("PSI-MS", "MS:1001062", "Mascot MGF file", CvParameter.DEFAULT_VALUE);
        MsRunIdFormat msRunIdFormat = new MsRunIdFormat("PSI-MS", "MS:1000774", "multiple peak list nativeID format", CvParameter.DEFAULT_VALUE);
        MsRun msRun = new MsRun(msRunFormat, msRunIdFormat, new URL("file:/test.file"));
        metaData.updateMsRun(msRun, 1);
        metaData.validate(new MzTabDocument(), new OneTimeDefaultValidatorMzTabSectionValidator());
    }
}