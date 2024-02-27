//package uk.ac.ebi.pride.gui.form.table.renderer;
//
//import uk.ac.ebi.pride.App;
//import uk.ac.ebi.pride.AppContext;
//import uk.ac.ebi.pride.data.model.DataFile;
//import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
//
//
//import javax.swing.*;
//import javax.swing.table.TableCellRenderer;
//import java.awt.*;
//
///**
// * Cell renderer to highlighting the number of file mappings
// *
// * @author Rui Wang
// * @version $Id$
// */
//public class FileMappingCountCellRenderer extends JLabel implements TableCellRenderer {
//
//    private static final Color CORRECT_FILE_MAPPING_COLOUR = new Color(40, 175, 99, 100);
//    private static final Color INCORRECT_FILE_MAPPING_COLOUR = new Color(215, 39, 41, 100);
//
//
//    private int count;
//    private boolean valid;
//    private AppContext appContext;
//
//    public FileMappingCountCellRenderer() {
//        this.setOpaque(true);
//        this.setHorizontalTextPosition(SwingConstants.CENTER);
//        this.setFont(this.getFont().deriveFont(Font.BOLD));
//        this.appContext = (AppContext) App.getInstance().getDesktopContext();
//    }
//
//    @Override
//    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//        valid = false;
//        count = (Integer)value;
//        java.util.List<DataFile> dataFiles = appContext.getSubmissionType().equals(SubmissionTypeConstants.COMPLETE) ?
//                                                appContext.getSubmissionFilesByType(ProjectFileType.RESULT) :
//                                                appContext.getSubmissionFilesByType(ProjectFileType.SEARCH);
//
//        if (!dataFiles.isEmpty() && row >= 0 && column >= 0) {
//            DataFile dataFile = dataFiles.get(row);
//            valid = dataFile.hasRawMappings();
//        }
//        return this;
//    }
//
//    @Override
//    protected void paintComponent(Graphics g) {
//        super.paintComponent(g);
//
//        Graphics2D g2 = (Graphics2D) g.create();
//
//        int width = getWidth();
//        int height = getHeight();
//
//        // rendering hints
//        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//
//        // paint a background
//        Color background;
//
//        if (valid) {
//            background = CORRECT_FILE_MAPPING_COLOUR;
//        } else {
//            background = INCORRECT_FILE_MAPPING_COLOUR;
//        }
//
//        if (background != null) {
//            g2.setColor(background);
//            g2.fillRect(0, 0, width, height);
//        }
//
//        // paint text
//        g2.setColor(Color.black);
//        g2.setFont(g2.getFont().deriveFont(Font.BOLD));
//        FontMetrics fontMetrics = g2.getFontMetrics();
//
//        String text = count + "";
//        int textWidth = fontMetrics.stringWidth(text);
//        int xPos = (width - textWidth) / 2;
//        int yPos = height / 2 + fontMetrics.getDescent() + 2;
//        g2.drawString(text, xPos, yPos);
//
//        g2.dispose();
//    }
//}
