package uk.ac.ebi.pride.gui.form.combo.model;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.util.Constant;

import java.util.Map;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class ExperimentMethodSelectionModel extends AbstractMetadataComboSelectionModel {

    public ExperimentMethodSelectionModel() {
        super(App.getInstance().getDesktopContext().getProperty("experiment.method.combobox.default.selection"),
              null,
              App.getInstance().getDesktopContext().getProperty("experiment.method.combobox.default.experiment.method.file"),
              Constant.MS);
    }

    public void addItem(CvParam cvParam) {
        appContext.addMassSpecExperimentMethod(cvParam);
    }
}
