package uk.ac.ebi.pride.gui.form.table.renderer;

import uk.ac.ebi.pride.gui.form.table.model.FileSelectionTableModel;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Renderer for highlighting invalid files
 *
 * @author Rui Wang
 * @version $Id$
 */
public class InvalidFileSelectionRenderer extends JLabel implements TableCellRenderer {

    private static final Color INVALID_FILE_COLOUR = new Color(215, 39, 41, 100);

    private boolean valid;
    private String text;


    public InvalidFileSelectionRenderer() {
        this.setOpaque(true);
        this.setForeground(Color.black);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        int modelRowIndex = table.convertRowIndexToModel(row);
        int modelColumnIndex = getModelColumnIndex();
        valid = (Boolean) table.getModel().getValueAt(modelRowIndex, modelColumnIndex);
        text = value == null ? "" : value.toString();
        if (!text.equals("")) {
            setToolTipText(text);
        }
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        int width = getWidth();
        int height = getHeight();

        // rendering hints
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // paint a background
        if (!valid) {
            g2.setColor(INVALID_FILE_COLOUR);
            g2.fillRect(0, 0, width, height);
        }

        // paint text
        g2.setColor(Color.black);
        FontMetrics fontMetrics = g2.getFontMetrics();

        int xPos = 5;
        int yPos = height / 2 + fontMetrics.getDescent() + 2;
        g2.drawString(text, xPos, yPos);

        g2.dispose();
    }

    private int getModelColumnIndex() {
        int index = -1;

        FileSelectionTableModel.TableHeader[] headers = FileSelectionTableModel.TableHeader.values();
        for (int i = 0; i < headers.length; i++) {
            if (FileSelectionTableModel.TableHeader.VALIDATION.equals(headers[i])) {
                index = i;
                break;
            }
        }

        return index;
    }
}