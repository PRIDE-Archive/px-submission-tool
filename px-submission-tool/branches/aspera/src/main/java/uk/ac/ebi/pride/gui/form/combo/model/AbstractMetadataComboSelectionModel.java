package uk.ac.ebi.pride.gui.form.combo.model;

import no.uib.olsdialog.OLSDialog;
import no.uib.olsdialog.OLSInputable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.gui.EDTUtils;
import uk.ac.ebi.pride.gui.util.CvFileUtil;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Abstract combo selection model for metadata selection
 *
 * @author Rui Wang
 * @version $Id$
 */
public abstract class AbstractMetadataComboSelectionModel extends AbstractListModel implements ComboBoxModel, OLSInputable {
    private static final Logger logger = LoggerFactory.getLogger(AbstractMetadataComboSelectionModel.class);

    protected List elements;
    protected Object selectedElement;
    protected AppContext appContext;
    private String ontology;
    private String otherSelectionOption;

    protected AbstractMetadataComboSelectionModel(String defaultSelectionOption,
                                                  String otherSelectionOption,
                                                  String defaultValueFile,
                                                  String ontology) {
        this.elements = new ArrayList();
        this.appContext = (AppContext) App.getInstance().getDesktopContext();

        // add default selection element
        elements.add(defaultSelectionOption);

        // default selected item
        selectedElement = elements.get(0);

        // add all the element from a template file
        try {
            elements.addAll(CvFileUtil.parseByTabDelimitedLine(defaultValueFile));
        } catch (IOException ioe) {
            logger.error("Failed to load default values from the template file", ioe);
        }

        // add element for calling ols dialog
        this.otherSelectionOption = otherSelectionOption;
        elements.add(otherSelectionOption);

        // default ontology to check
        this.ontology = ontology;
    }

    public abstract void addItem(CvParam cvParam);

    @Override
    public void setSelectedItem(Object anItem) {
        if ((selectedElement != null && !selectedElement.equals(anItem)) ||
                selectedElement == null && anItem != null) {

            if (anItem instanceof CvParam) {
                addItem((CvParam) anItem);
            } else if (otherSelectionOption.equals(anItem)) {
                Runnable run = new Runnable() {
                    @Override
                    public void run() {
                        new OLSDialog(((App) App.getInstance()).getMainFrame(), AbstractMetadataComboSelectionModel.this, true, "", ontology, "");
                    }
                };
                EDTUtils.invokeLater(run);
            }
        }
    }

    @Override
    public void insertOLSResult(String s, String s1, String s2, String s3, String s4, int i, String s5, Map<String, String> stringStringMap) {
        CvParam cvParam = new CvParam(s3, s2, s1, null);
        addItem(cvParam);
    }

    @Override
    public Window getWindow() {
        return ((App) App.getInstance()).getMainFrame();
    }

    @Override
    public int getSize() {
        return elements.size();
    }

    @Override
    public Object getElementAt(int index) {
        if (index >= 0 && index < elements.size())
            return elements.get(index);
        else
            return null;
    }

    @Override
    public Object getSelectedItem() {
        return selectedElement;
    }
}

