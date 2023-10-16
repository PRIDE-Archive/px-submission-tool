package uk.ac.ebi.pride.gui.form.combo.model;

import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.gui.form.table.model.MetaDataTableModel;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class ResultFileSelectionModel extends AbstractMetadataComboSelectionModel{
    private MetaDataTableModel tableModel;

    public ResultFileSelectionModel(String defaultSelectionOption,
                                    String otherSelectionOption,
                                    String defaultValueFile,
                                    String ontology,
                                    MetaDataTableModel tableModel) {
        super(defaultSelectionOption, otherSelectionOption, defaultValueFile, ontology);
        this.tableModel = tableModel;
    }

    public void addItem(CvParam cvParam) {
        tableModel.addValue(cvParam);
    }
}
