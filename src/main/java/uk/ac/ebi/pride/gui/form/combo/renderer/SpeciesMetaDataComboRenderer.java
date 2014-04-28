package uk.ac.ebi.pride.gui.form.combo.renderer;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.model.CvParam;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class SpeciesMetaDataComboRenderer extends AbstractMetadataComboRenderer {
    @Override
    protected boolean alreadySelected(Object value) {
        return value instanceof CvParam && ((AppContext) App.getInstance().getDesktopContext()).hasSpecies((CvParam) value);
    }
}
