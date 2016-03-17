package uk.ac.ebi.pride.gui.form.combo.model;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.util.Constant;

import java.util.Map;

/**
 * Combo box model for modification selection
 *
 * @author Rui Wang
 * @version $Id$
 */
public class ModificationSelectionModel extends AbstractMetadataComboSelectionModel {

    public ModificationSelectionModel() {
        super(App.getInstance().getDesktopContext().getProperty("modification.combobox.default.selection"),
                App.getInstance().getDesktopContext().getProperty("modification.combobox.other.modification"),
                App.getInstance().getDesktopContext().getProperty("modification.combobox.default.modification.file"),
                Constant.PSI_MOD);
    }

    public void addItem(CvParam cvParam) {
        appContext.addModification(cvParam);
    }
}
