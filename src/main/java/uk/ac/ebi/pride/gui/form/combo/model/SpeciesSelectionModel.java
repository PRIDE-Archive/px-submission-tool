package uk.ac.ebi.pride.gui.form.combo.model;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.util.Constant;

/**
 * Combo box model for species selection
 *
 * @author Rui Wang
 * @version $Id$
 */
public class SpeciesSelectionModel extends AbstractMetadataComboSelectionModel {

    public SpeciesSelectionModel() {
        super(App.getInstance().getDesktopContext().getProperty("species.combobox.default.selection"),
                App.getInstance().getDesktopContext().getProperty("species.combobox.other.species"),
                App.getInstance().getDesktopContext().getProperty("species.combobox.default.species.file"),
                Constant.PSI_MOD);
    }

    public void addItem(CvParam cvParam) {
        appContext.addSpecies(cvParam);
    }
}
