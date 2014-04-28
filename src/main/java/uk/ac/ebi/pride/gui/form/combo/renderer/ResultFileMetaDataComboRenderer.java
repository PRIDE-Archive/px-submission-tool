package uk.ac.ebi.pride.gui.form.combo.renderer;

import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.gui.form.table.model.MetaDataTableModel;

import java.util.Set;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class ResultFileMetaDataComboRenderer extends AbstractMetadataComboRenderer {
    private MetaDataTableModel tableModel;

    public ResultFileMetaDataComboRenderer(MetaDataTableModel tableModel) {
        super();
        this.tableModel = tableModel;
    }

    @Override
    protected boolean alreadySelected(Object value) {
        Set<CvParam> values = tableModel.getValues();
        return values != null && values.contains(value);
    }
}
