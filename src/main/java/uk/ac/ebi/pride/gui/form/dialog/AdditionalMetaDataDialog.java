package uk.ac.ebi.pride.gui.form.dialog;

import net.java.balloontip.BalloonTip;
import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.AppContext;
import uk.ac.ebi.pride.data.model.ProjectMetaData;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.data.validation.SubmissionValidator;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.toolsuite.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.gui.form.comp.HeaderPanel;
import uk.ac.ebi.pride.gui.form.comp.NonOpaquePanel;
import uk.ac.ebi.pride.gui.navigation.NavigationControlPanel;
import uk.ac.ebi.pride.gui.util.BalloonTipUtil;
import uk.ac.ebi.pride.gui.util.ColourUtil;
import uk.ac.ebi.pride.gui.util.Constant;

import javax.help.HelpBroker;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dialog for input additional metadata, such as: pubmed id, proteomexchange accession
 *
 * @author Rui Wang
 * @version $Id$
 */
public class AdditionalMetaDataDialog extends JDialog implements ActionListener {

    public static final String PUBMED_ID_PROPERTY = "pubmedIdProp";
    public static final String REANALYSIS_PX_ACC_PROPERTY = "reanalysisPxAccProp";
    public static final String OTHER_OMICS_LINK_PROPERTY = "otherOmicsLinkProp";

    private static final Pattern PUBMED_PATTERN = Pattern.compile("^\\d[\\d, ]+\\d$");
    private static final Pattern REANALYSIS_PX_ACC_PATTERN = Pattern.compile("^(PXD\\d{6}[, ]*)+$");

    private static final String HELP_ACTION_COMMAND = "helpAction";
    private static final String CANCEL_ACTION_COMMAND = "cancelAction";
    private static final String SET_ACTION_COMMAND = "setAction";

    /**
     * Pubmed text field
     */
    private JTextField pubmedTextField;

    /**
     * Reanalysis ProteomeXchange accession text field
     */
    private JTextField reanalysisPxTextField;

    /**
     * Other omics link text field
     */
    private JTextField otherOmicsTextField;

    /**
     * Warning balloon tip
     */
    private BalloonTip warningBalloonTip;


    public AdditionalMetaDataDialog(Frame owner) {
        super(owner);
        initComponents();
    }

    public AdditionalMetaDataDialog(Dialog owner) {
        super(owner);
        initComponents();
        postComponents();
    }

    private void postComponents() {
        AppContext context = (AppContext) App.getInstance().getDesktopContext();
        Submission submission = context.getSubmissionRecord().getSubmission();
        ProjectMetaData metaData = submission.getProjectMetaData();
        if (metaData != null) {
            java.util.Set<String> pubmedIds = metaData.getPubmedIds();
            if (!pubmedIds.isEmpty()) {
                String pubmedText = "";
                for (String pubmedId : pubmedIds) {
                    pubmedText += pubmedId + Constant.COMMA;
                }
                pubmedText = pubmedText.substring(0, pubmedText.length());
                pubmedTextField.setText(pubmedText);
            }

        }
    }

    /**
     * Create GUI components
     */
    private void initComponents() {
        this.setSize(new Dimension(500, 450));

        this.setTitle(App.getInstance().getDesktopContext().getProperty("additional.metadata.dialog.title"));

        JPanel contentPanel = new JPanel(new BorderLayout());
        this.setContentPane(contentPanel);

        // create title panel
        initTitlePanel();

        // create table panel
        initTextFieldPanel();

        // create button panel
        initControlPanel();

        // create balloon tip
        initBalloonTip();

        this.setContentPane(contentPanel);
    }

    /**
     * Initialize title field panel
     */
    private void initTitlePanel() {
        JPanel titlePanel = new HeaderPanel();
        // layout
        titlePanel.setLayout(new BorderLayout());
        // border
        titlePanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JPanel layoutPanel = new NonOpaquePanel(new FlowLayout(FlowLayout.LEFT));

        // title label
        JLabel titleLabel = new JLabel(App.getInstance().getDesktopContext().getProperty("additional.metadata.dialog.title"));
        titleLabel.setForeground(Color.white);
        titleLabel.setFont(titleLabel.getFont().deriveFont(15f).deriveFont(Font.BOLD));
        layoutPanel.add(titleLabel);

        titlePanel.add(layoutPanel, BorderLayout.CENTER);

        this.getContentPane().add(titlePanel, BorderLayout.NORTH);
    }

    /**
     * Initialize input text field panel
     */
    private void initTextFieldPanel() {
        JPanel textFieldPanel = new JPanel(new GridBagLayout());
        textFieldPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        // create pubmed id title label
        JLabel pubmedLabel = new JLabel(App.getInstance().getDesktopContext().getProperty("pubmed.id.title"));
        pubmedLabel.setFont(pubmedLabel.getFont().deriveFont(Font.BOLD));
        textFieldPanel.add(pubmedLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        // create pubmed text filed
        pubmedTextField = new JTextField();
        pubmedTextField.setPreferredSize(new Dimension(480, 40));
        textFieldPanel.add(pubmedTextField, c);

        // add gap
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        textFieldPanel.add(Box.createRigidArea(new Dimension(20, 20)), c);

        // reanalysis px accession title label
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        JLabel pxLabel = new JLabel(App.getInstance().getDesktopContext().getProperty("px.acc.reanalysis.title"));
        pxLabel.setFont(pxLabel.getFont().deriveFont(Font.BOLD));
        textFieldPanel.add(pxLabel, c);

        // reanalysis px text field
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        reanalysisPxTextField = new JTextField();
        reanalysisPxTextField.setPreferredSize(new Dimension(480, 40));
        textFieldPanel.add(reanalysisPxTextField, c);

        // add gap
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 1;
        textFieldPanel.add(Box.createRigidArea(new Dimension(20, 20)), c);

        // other omics link title label
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 1;
        JLabel otherOmicsLabel = new JLabel(App.getInstance().getDesktopContext().getProperty("other.omics.link.title"));
        otherOmicsLabel.setFont(pxLabel.getFont().deriveFont(Font.BOLD));
        textFieldPanel.add(otherOmicsLabel, c);

        // other omics link text field
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 2;
        otherOmicsTextField = new JTextField();
        otherOmicsTextField.setPreferredSize(new Dimension(480, 40));
        textFieldPanel.add(otherOmicsTextField, c);

        this.getContentPane().add(textFieldPanel, BorderLayout.CENTER);
    }

    /**
     * Initialize control panel
     */
    private void initControlPanel() {
        // setup main pane
        JPanel controlPanel = new NavigationControlPanel();
        controlPanel.setLayout(new BorderLayout());

        // app context
        DesktopContext appContext = App.getInstance().getDesktopContext();

        // help button
        JPanel helpButtonPanel = new NonOpaquePanel(new FlowLayout(FlowLayout.LEFT));
        JButton helpButton = GUIUtilities.createLabelLikeButton(GUIUtilities.loadIcon(appContext.getProperty("help.button.small.icon")), null);
        helpButton.setPreferredSize(new Dimension(90, 30));
        helpButton.setFocusable(false);
        helpButton.setActionCommand(HELP_ACTION_COMMAND);
        helpButton.addActionListener(this);
        helpButtonPanel.add(helpButton);
        controlPanel.add(helpButtonPanel, BorderLayout.WEST);

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
        JButton addButton = new JButton(appContext.getProperty("set.additional.metadata.button.label"),
                GUIUtilities.loadIcon(appContext.getProperty("set.additional.metadata.button.small.icon")));
        addButton.setPreferredSize(new Dimension(90, 30));
        addButton.setActionCommand(SET_ACTION_COMMAND);
        addButton.addActionListener(this);
        ctrlPane.add(addButton);

        controlPanel.add(ctrlPane, BorderLayout.EAST);

        this.getContentPane().add(controlPanel, BorderLayout.SOUTH);
    }

    /**
     * Initialize balloon warning tip
     */
    private void initBalloonTip() {
        this.warningBalloonTip = BalloonTipUtil.createErrorBalloonTip(pubmedTextField, "");
        this.warningBalloonTip.setPadding(2);
        this.warningBalloonTip.setVisible(false);

        // tooltip
        BalloonTipUtil.createBalloonTooltip(pubmedTextField, App.getInstance().getDesktopContext().getProperty("pubmed.id.tooltip"));
        BalloonTipUtil.createBalloonTooltip(reanalysisPxTextField, App.getInstance().getDesktopContext().getProperty("px.acc.reanalysis.tooltip"));
        BalloonTipUtil.createBalloonTooltip(otherOmicsTextField, App.getInstance().getDesktopContext().getProperty("other.omics.link.tooltip"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String evtName = e.getActionCommand();

        AppContext appContext = (AppContext) App.getInstance().getDesktopContext();
        if (HELP_ACTION_COMMAND.equals(evtName)) {
            HelpBroker hb = appContext.getMainHelpBroker();
            hb.showID("help.meta.data", "javax.help.SecondaryWindow", "main");
        } else if (CANCEL_ACTION_COMMAND.equals(evtName)) {
            this.dispose();
        } else if (SET_ACTION_COMMAND.equals(evtName)) {
            String pubMedIds = pubmedTextField.getText();
            if (pubMedIds != null) {
                pubMedIds = pubMedIds.trim();
                if (pubMedIds.length() > 0) {
                    Matcher m = PUBMED_PATTERN.matcher(pubMedIds);
                    if (m.find()) {
                        warningBalloonTip.setVisible(false);
                        pubmedTextField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
                        firePropertyChange(PUBMED_ID_PROPERTY, null, pubMedIds);
                    } else {
                        showWarning(pubmedTextField, appContext.getProperty("pubmed.id.error.message"));
                        return;
                    }
                }
            }

            String pxAccs = reanalysisPxTextField.getText();
            if (pxAccs != null) {
                pxAccs = pxAccs.trim();
                if (pxAccs.length() > 0) {
                    Matcher m = REANALYSIS_PX_ACC_PATTERN.matcher(pxAccs);
                    if (m.find()) {
                        warningBalloonTip.setVisible(false);
                        reanalysisPxTextField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
                        firePropertyChange(REANALYSIS_PX_ACC_PROPERTY, null, pxAccs);
                    } else {
                        showWarning(reanalysisPxTextField, appContext.getProperty("px.acc.reanalysis.error.message"));
                        return;
                    }
                }
            }

            String otherOmicsLink = otherOmicsTextField.getText();
            if (SubmissionValidator.validateOtherOmicsLink(otherOmicsLink).hasError()) {
                showWarning(otherOmicsTextField, appContext.getProperty("other.omics.link.error.message"));
                return;
            } else {
                warningBalloonTip.setVisible(false);
                otherOmicsTextField.setBackground(ColourUtil.TEXT_FIELD_NORMAL_COLOUR);
                firePropertyChange(OTHER_OMICS_LINK_PROPERTY, null, otherOmicsLink);
            }

            this.setVisible(false);
        }
    }

    /**
     * Show warning message
     *
     * @param component attachment component
     * @param message   warning message
     */
    private void showWarning(JComponent component, String message) {
        component.setBackground(ColourUtil.TEXT_FIELD_WARNING_COLOUR);
        // set attachment component
        warningBalloonTip.setAttachedComponent(component);

        // show balloon warning
        warningBalloonTip.setContents(new JLabel(message));
        warningBalloonTip.setVisible(true);
    }
}
