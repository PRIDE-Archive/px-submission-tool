package uk.ac.ebi.pride.gui.form.table.listener;


import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Listens to mouse move event and change mouse cursor accordingly.
 *
 * @author Rui Wang
 * @version $Id$
 */
public class TableCellMouseMotionListener extends MouseAdapter {

    private JTable table;
    private java.util.List<String> columnHeader;

    public TableCellMouseMotionListener(JTable table, String... columnHeader) {
        this.table = table;
        this.columnHeader = new ArrayList<String>(Arrays.asList(columnHeader));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
        if (col >= 0) {
            String header = table.getColumnName(col);
            if (columnHeader.contains(header)) {
                table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                table.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }
}
