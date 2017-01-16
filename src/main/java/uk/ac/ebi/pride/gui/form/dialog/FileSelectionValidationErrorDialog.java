package uk.ac.ebi.pride.gui.form.dialog;

import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareDialog;
import uk.ac.ebi.pride.gui.form.comp.NonOpaquePanel;
import uk.ac.ebi.pride.gui.form.table.PxTable;
import uk.ac.ebi.pride.gui.navigation.NavigationControlPanel;
import uk.ac.ebi.pride.gui.util.DataFileValidationMessage;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * @author Rui Wang
 * @version $Id$
 */
public class FileSelectionValidationErrorDialog extends ContextAwareDialog implements ActionListener {
    private static final String CLOSE_ACTION = "closeAction";

    private DataFileValidationMessage validationMessage;

    public FileSelectionValidationErrorDialog(Frame owner, DataFileValidationMessage validationMessage) {
        super(owner);
        this.validationMessage = validationMessage;
        initComponents();
    }

    /**
     * Create GUI components
     */
    private void initComponents() {
        this.setSize(new Dimension(400, 400));

        JPanel contentPanel = new JPanel(new BorderLayout());
        this.setContentPane(contentPanel);

        // create table panel
        initTablePanel();

        // create button panel
        initControlPanel();

        this.setContentPane(contentPanel);
    }

    /**
     * Initialize table panel
     */
    private void initTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // create table title label
        JLabel label = new JLabel(validationMessage.getMessage());
        label.setIcon(GUIUtilities.loadIcon(appContext.getProperty("warning.message.icon")));
        label.setFont(label.getFont().deriveFont(Font.BOLD).deriveFont(14f));
        tablePanel.add(label, BorderLayout.NORTH);

        // create table
        JTable warningTable = new PxTable(new FileSelectionValidationTableModel(validationMessage.getDataFileValidationResults()));
        warningTable.setTableHeader(null);
        warningTable.setBorder(BorderFactory.createLineBorder(Color.gray));

        // scroll pane
        JScrollPane scrollPane = new JScrollPane(warningTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        tablePanel.add(scrollPane, BorderLayout.CENTER);

        this.getContentPane().add(tablePanel, BorderLayout.CENTER);
    }

    /**
     * Initialize control panel
     */
    private void initControlPanel() {
        // setup main pane
        JPanel controlPanel = new NavigationControlPanel();
        controlPanel.setLayout(new BorderLayout());

//        // help button
//        JPanel helpButtonPanel = new NonOpaquePanel(new FlowLayout(FlowLayout.LEFT));
//        JButton helpButton = GUIUtilities.createLabelLikeButton(GUIUtilities.loadIcon(appContext.getProperty("help.button.small.icon")), null);
//        helpButton.setPreferredSize(new Dimension(90, 30));
//        helpButton.setFocusable(false);
//        helpButton.setActionCommand(HELP_ACTION_COMMAND);
//        helpButton.addActionListener(this);
//        helpButtonPanel.add(helpButton);
//        controlPanel.add(helpButtonPanel, BorderLayout.WEST);

        // control pane
        JPanel ctrlPane = new NonOpaquePanel(new FlowLayout(FlowLayout.RIGHT));

        // cancel button
        JButton closeButton = new JButton(appContext.getProperty("close.button.label"),
                GUIUtilities.loadIcon(appContext.getProperty("close.button.small.icon")));
        closeButton.setPreferredSize(new Dimension(90, 30));
        closeButton.setActionCommand(CLOSE_ACTION);
        closeButton.addActionListener(this);
        ctrlPane.add(closeButton);
        controlPanel.add(ctrlPane, BorderLayout.EAST);

        this.getContentPane().add(controlPanel, BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String evtName = e.getActionCommand();

        if (CLOSE_ACTION.equals(evtName)) {
            this.dispose();
        }
    }


    private class FileSelectionValidationTableModel extends AbstractTableModel {

        private java.util.Set<String> missingFiles;

        public FileSelectionValidationTableModel(Map<DataFile, java.util.List<String>> dataFileWarningResults) {
            this.missingFiles = new LinkedHashSet<String>();
            for (List<String> mfs : dataFileWarningResults.values()) {
                this.missingFiles.addAll(mfs);
            }
        }


        @Override
        public int getRowCount() {
            return missingFiles.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return new ArrayList<String>(this.missingFiles).get(rowIndex);
        }
    }
}
