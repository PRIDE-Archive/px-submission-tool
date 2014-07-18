package uk.ac.ebi.pride.gui.form.dialog;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareDialog;
import uk.ac.ebi.pride.gui.form.comp.NonOpaquePanel;
import uk.ac.ebi.pride.gui.form.table.TableFactory;
import uk.ac.ebi.pride.gui.form.table.model.FileMappingTableModel;
import uk.ac.ebi.pride.gui.navigation.NavigationControlPanel;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;
import uk.ac.ebi.pride.archive.dataprovider.project.SubmissionType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * Dialog for selecting mapping files
 *
 * @author Rui Wang
 * @version $Id$
 */
public class FileMappingDialog extends ContextAwareDialog implements ActionListener {
    private static final String CANCEL_ACTION_COMMAND = "cancelAction";
    private static final String ADD_ACTION_COMMAND = "addAction";

    /**
     * File mapping table
     */
    private JTable fileMappingTable;

    /**
     * The data file which the mapping is going to be added
     */
    private DataFile dataFile;

    /**
     * File mapping table model
     */
    private FileMappingTableModel fileMappingTableModel;

    public FileMappingDialog(Frame owner, DataFile dataFile) {
        super(owner);
        this.dataFile = dataFile;
        initComponents();
        postComponents();
    }

    public FileMappingDialog(Dialog owner, DataFile dataFile) {
        super(owner);
        this.dataFile = dataFile;
        initComponents();
        postComponents();
    }

    /**
     * Create GUI components
     */
    private void initComponents() {
        this.setSize(new Dimension(850, 400));

        JPanel contentPanel = new JPanel(new BorderLayout());
        this.setContentPane(contentPanel);

        // create table panel
        initTablePanel();

        // create button panel
        initControlPanel();

        this.setContentPane(contentPanel);
    }

    /**
     * Post component creation, populate the table with content
     */
    private void postComponents() {
        Submission submission = appContext.getSubmissionRecord().getSubmission();
        java.util.List<DataFile> dataFiles = submission.getDataFiles();
        for (DataFile dataFile : dataFiles) {
            if ((appContext.getSubmissionType().equals(SubmissionType.COMPLETE) && !dataFile.getFileType().equals(ProjectFileType.RESULT)) ||
                    (appContext.getSubmissionType().equals(SubmissionType.PARTIAL) && !dataFile.getFileType().equals(ProjectFileType.SEARCH))) {
                fileMappingTableModel.addData(dataFile, this.dataFile.containsFileMapping(dataFile));
            }
        }
    }

    /**
     * Initialize table panel
     */
    private void initTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // create table title label
        JLabel label = new JLabel(App.getInstance().getDesktopContext().getProperty("file.mapping.table.title"));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        tablePanel.add(label, BorderLayout.NORTH);

        // create table
        fileMappingTable = TableFactory.createFileMappingTable();

        // get table model
        fileMappingTableModel = (FileMappingTableModel) fileMappingTable.getModel();

        // scroll pane
        JScrollPane scrollPane = new JScrollPane(fileMappingTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

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
        JButton cancelButton = new JButton(appContext.getProperty("cancel.button.label"),
                GUIUtilities.loadIcon(appContext.getProperty("cancel.button.small.icon")));
        cancelButton.setPreferredSize(new Dimension(90, 30));
        cancelButton.setActionCommand(CANCEL_ACTION_COMMAND);
        cancelButton.addActionListener(this);
        ctrlPane.add(cancelButton);

        // next button
        JButton addButton = new JButton(appContext.getProperty("add.file.mapping.button.label"),
                GUIUtilities.loadIcon(appContext.getProperty("add.file.mapping.dialog.button.small.icon")));
        addButton.setPreferredSize(new Dimension(90, 30));
        addButton.setActionCommand(ADD_ACTION_COMMAND);
        addButton.addActionListener(this);
        ctrlPane.add(addButton);

        controlPanel.add(ctrlPane, BorderLayout.EAST);

        this.getContentPane().add(controlPanel, BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String evtName = e.getActionCommand();

        if (CANCEL_ACTION_COMMAND.equals(evtName)) {
            this.dispose();
        } else if (ADD_ACTION_COMMAND.equals(evtName)) {
            AppContext appContext = ((AppContext) App.getInstance().getDesktopContext());
            Map<DataFile, Boolean> data = fileMappingTableModel.getData();
            appContext.removeAllFileMappings(dataFile);
            for (Map.Entry<DataFile, Boolean> dataFileBooleanEntry : data.entrySet()) {
                if (dataFileBooleanEntry.getValue()) {
                    appContext.addFileMapping(dataFile, dataFileBooleanEntry.getKey());
                }
            }

            this.dispose();
        }
    }
}
