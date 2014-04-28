package uk.ac.ebi.pride.gui.form.table.renderer;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.gui.GUIUtilities;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Cell renderer for removal column, which user can click to delete a row from the table
 *
 * @author Rui Wang
 * @version $Id$
 */
public class RemovalCellRenderer extends JLabel implements TableCellRenderer {

    private ImageIcon icon;

    public RemovalCellRenderer() {
        this.setOpaque(true);
        // load icon image
        this.icon = GUIUtilities.loadImageIcon(App.getInstance().getDesktopContext().getProperty("table.remove.row.small.icon"));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        //create a new graphics
        Graphics2D g2d = (Graphics2D)g.create();

        // set rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // calculate drawing position
        int width = getWidth();
        int height = getHeight();
        int iconWidth = icon.getIconWidth();
        int iconHeight = icon.getIconHeight();

        int xPos = width/2 - iconWidth/2;
        int yPos = height/2 - iconHeight/2;

        // draw icon image
        g2d.drawImage(icon.getImage(), xPos, yPos, null);

        //dispose graphics
        g2d.dispose();
    }
}
