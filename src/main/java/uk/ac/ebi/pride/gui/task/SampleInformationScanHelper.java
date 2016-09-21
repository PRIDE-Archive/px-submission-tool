package uk.ac.ebi.pride.gui.task;

import uk.ac.ebi.pride.data.model.SampleMetaData;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.task
 * Timestamp: 2016-07-28 9:54
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 *
 * This class is a refactoring via externalization of some of the algorithms for processing and manipulating
 * SampleMetaData and SampleDescription information
 */
public class SampleInformationScanHelper {

    /**
     * Get sample metadata type
     */
    public static SampleMetaData.Type getSampleMetaDataType(String cvLabel) {
        SampleMetaData.Type type = null;

        if (cvLabel.equalsIgnoreCase(uk.ac.ebi.pride.data.util.Constant.NEWT)) {
            type = SampleMetaData.Type.SPECIES;
        } else if (cvLabel.equalsIgnoreCase(uk.ac.ebi.pride.data.util.Constant.BTO)) {
            type = SampleMetaData.Type.TISSUE;
        } else if (cvLabel.equalsIgnoreCase(uk.ac.ebi.pride.data.util.Constant.CL)) {
            type = SampleMetaData.Type.CELL_TYPE;
        } else if (cvLabel.equalsIgnoreCase(uk.ac.ebi.pride.data.util.Constant.DOID)) {
            type = SampleMetaData.Type.DISEASE;
        }

        return type;
    }
}
