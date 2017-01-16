package uk.ac.ebi.pride.gui.form.combo.renderer;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.data.ExtendedCvParam;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer to render the metadata selection combo box
 *
 * @author Rui Wang
 * @version $Id$
 */
public abstract class AbstractMetadataComboRenderer extends JLabel implements ListCellRenderer{
    public AbstractMetadataComboRenderer() {
        setOpaque(true);
        setHorizontalAlignment(LEFT);
        setVerticalAlignment(CENTER);
        setBorder(BorderFactory.createEmptyBorder(0, 5, 0 ,0));
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        AppContext appContext = (AppContext) App.getInstance().getDesktopContext();

        // display text
        if (value instanceof ExtendedCvParam) {
            this.setText(((ExtendedCvParam) value).getHumanReadableName());
        } else if (value instanceof CvParam) {
            this.setText(((CvParam) value).getName());
        } else if (value != null){
            this.setText(value.toString());
        }

        // display icon
        if (alreadySelected(value)) {
            this.setHorizontalTextPosition(JLabel.LEFT);
            this.setIcon(GUIUtilities.loadIcon(appContext.getProperty("combobox.tick.small.icon")));
            this.setIconTextGap(5);
        } else {
            this.setIcon(null);
        }

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(Color.white);
            setForeground(list.getForeground());
        }

        return this;
    }

    protected abstract boolean alreadySelected(Object value);
}
