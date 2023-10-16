package uk.ac.ebi.pride.gui.form.combo.model;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.util.Constant;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class QuantificationSelectionModel extends AbstractMetadataComboSelectionModel {

    public QuantificationSelectionModel() {
        super(App.getInstance().getDesktopContext().getProperty("quantification.combobox.default.selection"),
                App.getInstance().getDesktopContext().getProperty("quantification.combobox.other.quantification"),
                App.getInstance().getDesktopContext().getProperty("quantification.combobox.default.quantification.file"),
                Constant.PRIDE);
    }

    public void addItem(CvParam cvParam) {
        appContext.addQuantification(cvParam);
    }
}
