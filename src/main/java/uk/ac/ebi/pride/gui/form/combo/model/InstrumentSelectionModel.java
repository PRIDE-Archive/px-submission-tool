package uk.ac.ebi.pride.gui.form.combo.model;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.util.Constant;

/**
 * Combo box model for instrument selection
 *
 * @author Rui Wang
 * @version $Id$
 */
public class InstrumentSelectionModel extends AbstractMetadataComboSelectionModel {

    public InstrumentSelectionModel() {
        super(App.getInstance().getDesktopContext().getProperty("instrument.combobox.default.selection"),
                App.getInstance().getDesktopContext().getProperty("instrument.combobox.other.instrument"),
                App.getInstance().getDesktopContext().getProperty("instrument.combobox.default.instrument.file"),
                Constant.MS);
    }

    public void addItem(CvParam cvParam) {
        appContext.addInstrument(cvParam);
    }
}
