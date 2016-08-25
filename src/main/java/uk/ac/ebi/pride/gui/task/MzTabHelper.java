package uk.ac.ebi.pride.gui.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.SampleMetaData;
import uk.ac.ebi.pride.data.mztab.model.CvParameter;
import uk.ac.ebi.pride.data.mztab.model.MetaData;
import uk.ac.ebi.pride.data.mztab.model.MzTabDocument;
import uk.ac.ebi.pride.data.mztab.model.Sample;

import java.util.Set;

/**
 * Project: px-submission-tool
 * Package: uk.ac.ebi.pride.gui.task
 * Timestamp: 2016-07-28 10:14
 * ---
 * © 2016 Manuel Bernal Llinares <mbdebian@gmail.com>
 * All rights reserved.
 * <p>
 * This class is a Facade for processing and/or manipulating information in an MzTabDocument
 */
public class MzTabHelper {
    private static final Logger logger = LoggerFactory.getLogger(MzTabHelper.class);

    private static void addCvParameterToSampleMetaData(SampleMetaData sampleMetaData, CvParameter cvParameter, SampleMetaData.Type type) {
        if (cvParameter != null) {
            sampleMetaData.addMetaData(type,
                    new CvParam(cvParameter.getLabel(), cvParameter.getAccession(), cvParameter.getName(), cvParameter.getValue()));
        }
    }

    public static SampleMetaData getSampleMetaData(MzTabDocument mzTabDocument) {
        SampleMetaData sampleMetaData = new SampleMetaData();
        MetaData mzTabMetaData = mzTabDocument.getMetaData();
        if (mzTabMetaData != null) {
            for (int index :
                    mzTabMetaData.getAvailableSampleIndexes()) {
                Sample sample = mzTabMetaData.getSampleData(index);
                for (int dataEntryIndex :
                        sample.getDataEntryIndexes()) {
                    Sample.DataEntry dataEntry = sample.getDataEntry(dataEntryIndex);
                    addCvParameterToSampleMetaData(sampleMetaData, dataEntry.getCellType(), SampleMetaData.Type.CELL_TYPE);
                    addCvParameterToSampleMetaData(sampleMetaData, dataEntry.getDisease(), SampleMetaData.Type.DISEASE);
                    addCvParameterToSampleMetaData(sampleMetaData, dataEntry.getSpecies(), SampleMetaData.Type.SPECIES);
                    addCvParameterToSampleMetaData(sampleMetaData, dataEntry.getTissue(), SampleMetaData.Type.TISSUE);
                }
            }
            // Get quantification method
            addCvParameterToSampleMetaData(sampleMetaData, mzTabMetaData.getQuantificationMethod(), SampleMetaData.Type.QUANTIFICATION_METHOD);
            Set<Integer> instrumentIndexes = mzTabMetaData.getAvailableInstrumentEntryIndexes();
            if (instrumentIndexes.size() > 0) {
                // Process instrument information, basically by adding all its associated annotations to
                // Sample Metadata as INSTRUMENT metadata type
                for (int index :
                        instrumentIndexes) {
                    addCvParameterToSampleMetaData(sampleMetaData, mzTabMetaData.getInstrument(index).getName(), SampleMetaData.Type.INSTRUMENT);
                    addCvParameterToSampleMetaData(sampleMetaData, mzTabMetaData.getInstrument(index).getSource(), SampleMetaData.Type.INSTRUMENT);
                    addCvParameterToSampleMetaData(sampleMetaData, mzTabMetaData.getInstrument(index).getDetector(), SampleMetaData.Type.INSTRUMENT);
                    for (int analyzerIndex :
                            mzTabMetaData.getInstrument(index).getAvailableAnalyzerIndexes()) {
                        addCvParameterToSampleMetaData(sampleMetaData, mzTabMetaData.getInstrument(index).getAnalyzerEntry(analyzerIndex), SampleMetaData.Type.INSTRUMENT);
                    }
                }
            }
            // TODO - WARNING -- custom sample metadata type is allowed in mzTab files but not covered by SampleMetaData.Type
        }
        return sampleMetaData;
    }
}
