/*
 * Created by JFormDesigner on Sun Nov 20 12:20:36 GMT 2011
 */

package uk.ac.ebi.pride.gui.form.dialog;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.util.Constant;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareDialog;
import uk.ac.ebi.pride.gui.util.BalloonTipUtil;
import uk.ac.ebi.pride.gui.util.ColourUtil;
import uk.ac.ebi.pride.archive.dataprovider.file.ProjectFileType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Dialog for adding a URL
 *
 * @author Rui Wang
 * @version $Id$
 */
public class AddURLDialog extends ContextAwareDialog {

    public AddURLDialog(Frame owner) {
        super(owner);
        initComponents();
        postComponentCreation();
    }

    public AddURLDialog(Dialog owner) {
        super(owner);
        initComponents();
        postComponentCreation();
    }

    private void postComponentCreation() {
        warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(urlTextField, "");
        warningBalloonTip.setPadding(2);
        warningBalloonTip.setVisible(false);

        // add action listener
        addButton.addActionListener(new AddURLListener());

        // cancel action listener
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AddURLDialog.this.dispose();
            }
        });

        // populate combo box
        for (ProjectFileType massSpecFileType : ProjectFileType.values()) {
            typeComboBox.addItem(massSpecFileType);
        }
        typeComboBox.setSelectedIndex(-1);

    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        urlLabel = new JLabel();
        urlDescLabel = new JLabel();
        urlTextField = new JTextField();
        helpButton = new JButton();
        addButton = new JButton();
        typeLabel = new JLabel();
        typeComboBox = new JComboBox();
        typeDescLabel = new JLabel();
        cancelButton = new JButton();

        //======== this ========
        Container contentPane = getContentPane();

        //---- urlLabel ----
        urlLabel.setText("URL");
        urlLabel.setFont(new Font("sansserif", Font.BOLD, 12));

        //---- urlDescLabel ----
        urlDescLabel.setForeground(new Color(153, 153, 153));
        urlDescLabel.setText("(For example: file FTP address)");

        //---- helpButton ----
        helpButton.setText("Help");

        //---- addButton ----
        addButton.setText("Add");

        //---- typeLabel ----
        typeLabel.setText("Type");
        typeLabel.setFont(typeLabel.getFont().deriveFont(typeLabel.getFont().getStyle() | Font.BOLD));

        //---- typeDescLabel ----
        typeDescLabel.setText("(Select file type)");
        typeDescLabel.setForeground(new Color(153, 153, 153));

        //---- cancelButton ----
        cancelButton.setText("Cancel");

        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPane.setLayout(contentPaneLayout);
        contentPaneLayout.setHorizontalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(contentPaneLayout.createParallelGroup()
                                        .addGroup(contentPaneLayout.createSequentialGroup()
                                                .addComponent(urlLabel, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(urlDescLabel, GroupLayout.PREFERRED_SIZE, 287, GroupLayout.PREFERRED_SIZE))
                                        .addComponent(urlTextField, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 471, Short.MAX_VALUE)
                                        .addGroup(contentPaneLayout.createSequentialGroup()
                                                .addComponent(helpButton)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 283, Short.MAX_VALUE)
                                                .addComponent(cancelButton)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(addButton, GroupLayout.PREFERRED_SIZE, 63, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                                .addGroup(GroupLayout.Alignment.LEADING, contentPaneLayout.createSequentialGroup()
                                                        .addComponent(typeLabel, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(typeDescLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                .addComponent(typeComboBox, GroupLayout.Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 174, GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        );
        contentPaneLayout.setVerticalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addGap(22, 22, 22)
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(urlLabel)
                                        .addComponent(urlDescLabel))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(urlTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(contentPaneLayout.createParallelGroup()
                                        .addComponent(typeDescLabel)
                                        .addComponent(typeLabel))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(typeComboBox, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 25, Short.MAX_VALUE)
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(helpButton)
                                        .addComponent(addButton)
                                        .addComponent(cancelButton))
                                .addGap(18, 18, 18))
        );
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JLabel urlLabel;
    private JLabel urlDescLabel;
    private JTextField urlTextField;
    private JButton helpButton;
    private JButton addButton;
    private JLabel typeLabel;
    private JComboBox typeComboBox;
    private JLabel typeDescLabel;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    /**
     * Listener to call when add url button is clicked
     */
    private class AddURLListener implements ActionListener {
        private static final String TEXT_FIELD_WARNING_MESSAGE = "Please input a valid URL below";
        private static final String COMBO_BOX_WARNING_MESSAGE = "Please select a file type from below";


        @Override
        public void actionPerformed(ActionEvent e) {
            String url = urlTextField.getText().trim();
            if ("".equals(url)) {
                showWarning(urlTextField, TEXT_FIELD_WARNING_MESSAGE);
                urlTextField.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
            } else {
                if (Constant.URL_PATTERN.matcher(url).find()) {
                    Object type = typeComboBox.getSelectedItem();
                    if (type != null) {
                        // create a new data file
                        try {
                            ((AppContext) App.getInstance().getDesktopContext()).addDataFile(new DataFile(new URL(url), (ProjectFileType) type));
                            AddURLDialog.this.dispose();
                        } catch (MalformedURLException e1) {
                            showWarning(urlTextField, TEXT_FIELD_WARNING_MESSAGE);
                            urlTextField.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
                        }
                    } else {
                        showWarning(typeComboBox, COMBO_BOX_WARNING_MESSAGE);
                        urlTextField.setBackground(Color.white);
                    }

                } else {
                    showWarning(urlTextField, TEXT_FIELD_WARNING_MESSAGE);
                    urlTextField.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
                }
            }
        }

        /**
         * Show warning message
         *
         * @param component attachment component
         * @param message   warning message
         */
        private void showWarning(JComponent component, String message) {
            // set attachment component
            warningBalloonTip.setAttachedComponent(component);

            // show balloon warning
            warningBalloonTip.setContents(new JLabel(message));
            warningBalloonTip.setVisible(true);
        }
    }
}
