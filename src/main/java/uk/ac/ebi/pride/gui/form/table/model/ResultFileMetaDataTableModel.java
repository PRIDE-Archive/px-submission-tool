package uk.ac.ebi.pride.gui.form.table.model;

import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Param;
import uk.ac.ebi.pride.data.model.SampleMetaData;

import java.util.Set;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class ResultFileMetaDataTableModel extends MetaDataTableModel {

    public ResultFileMetaDataTableModel(DataFile dataFile, SampleMetaData.Type type) {
        super();
        Set<CvParam> params = dataFile.getSampleMetaData().getMetaData(type);
        if (params != null) {
            for (Param param : params) {
                addValue((CvParam) param);
            }
        }
    }
}
