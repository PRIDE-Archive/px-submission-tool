package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.form.action.LoadSubmissionFileAction;
import uk.ac.ebi.pride.gui.form.comp.GradientRoundedPanel;
import uk.ac.ebi.pride.gui.form.comp.RoundedPanel;
import uk.ac.ebi.pride.gui.form.dialog.ResubmissionDialog;
import uk.ac.ebi.pride.gui.util.BorderUtil;
import uk.ac.ebi.pride.gui.util.ColourUtil;
import uk.ac.ebi.pride.gui.util.HttpUtil;
import uk.ac.ebi.pride.prider.dataprovider.project.SubmissionType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * This form shows welcome messages
 *
 * @author Rui Wang
 * @version $Id$
 */
public class WelcomeForm extends Form {

    private static final String FULL_SUBMISSION_OPTION = "FULL_SUBMISSION";
    private static final String PARTIAL_SUBMISSION_OPTION = "PARTIAL_SUBMISSION";
    private static final String MINIMUM_SUBMISSION_OPTION = "MINIMUM_SUBMISSION";


    private JButton fullSubmissionButton;
    private JButton partialSubmissionButton;
    private JButton minimumSubmissionButton;

    private JPanel requirementItemContainer;
    private JPanel resultFileRequirementItemPanel;
    private JPanel identificationFileRequirementItemPanel;
    private JPanel rawFileRequirementItemPanel;
    private JPanel prideLoginRequirementItemPanel;

    private ResubmissionDialog resubmissionDialog;


    public WelcomeForm() {
        initComponents();
    }

    private void initComponents() {
        // setup the main pane
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        // create mission statement panel
        JPanel submissionOptionPanel = createSubmissionOptionPanel();
        this.add(submissionOptionPanel, BorderLayout.NORTH);

        // create requirement panel
        JPanel detailPanel = createRequirementPanel();
        this.add(detailPanel, BorderLayout.CENTER);

        // create requirement item panel
        resultFileRequirementItemPanel = createResultFilePanel();
        identificationFileRequirementItemPanel = createIdentificationFilePanel();
        rawFileRequirementItemPanel = createRawFilePanel();
        prideLoginRequirementItemPanel = createPrideLoginPanel();

        // select the default submission option
        fullSubmissionButton.doClick();
    }

    /**
     * Panel allows users to choose which type of submission they would like to perform
     */
    private JPanel createSubmissionOptionPanel() {
        JPanel optionPanel = new JPanel(new BorderLayout());
        optionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
//        optionPanel.setBorder(BorderUtil.createLoweredBorder());

        // title panel
        optionPanel.add(createOptionTitlePanel(), BorderLayout.NORTH);

        // button panel for choosing the submission type
        optionPanel.add(createOptionButtonPanel(), BorderLayout.CENTER);

        // external link panel
        optionPanel.add(createOptionLinkPanel(), BorderLayout.SOUTH);

        return optionPanel;
    }

    /**
     * Create a title panel for the option panel
     */
    private JPanel createOptionTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel(appContext.getProperty("welcome.before.start.title"));
        titleLabel.setFont(titlePanel.getFont().deriveFont(16f).deriveFont(Font.BOLD));
        titlePanel.add(titleLabel, BorderLayout.NORTH);
//        JLabel descPanel = new JLabel(appContext.getProperty("welcome.before.start.desc"));
//        titlePanel.add(descPanel, BorderLayout.CENTER);
        titlePanel.add(Box.createRigidArea(new Dimension(10, 10)), BorderLayout.SOUTH);

        return titlePanel;
    }

    /**
     * Create a button panel for the option panel
     */
    private JPanel createOptionButtonPanel() {
        JPanel buttonPanel = new JPanel();
        GridLayout gridLayout = new GridLayout(1, 3);
        gridLayout.setHgap(10);

        // action listener
        ActionListener submissionOptionListener = new SubmissionOptionListener();

        ButtonGroup buttonGroup = new ButtonGroup();

        // full submission button
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("welcome.full.submission.button.title.large.icon"));
        fullSubmissionButton = createOptionButton(appContext.getProperty("welcome.full.submission.button.title"), icon);
        fullSubmissionButton.setActionCommand(FULL_SUBMISSION_OPTION);
        fullSubmissionButton.addActionListener(submissionOptionListener);
        buttonPanel.add(fullSubmissionButton);
        buttonGroup.add(fullSubmissionButton);

        // partial submission button, contains unsupported files
        icon = GUIUtilities.loadIcon(appContext.getProperty("welcome.partial.submission.button.title.large.icon"));
        partialSubmissionButton = createOptionButton(appContext.getProperty("welcome.partial.submission.button.title"), icon);
        partialSubmissionButton.setActionCommand(PARTIAL_SUBMISSION_OPTION);
        partialSubmissionButton.addActionListener(submissionOptionListener);
        buttonPanel.add(partialSubmissionButton);
        buttonGroup.add(partialSubmissionButton);

        // minimum submission button
//        icon = GUIUtilities.loadIcon(appContext.getProperty("welcome.minimum.submission.button.title.large.icon"));
//        minimumSubmissionButton = createOptionButton(appContext.getProperty("welcome.minimum.submission.button.title"), icon);
//        minimumSubmissionButton.setActionCommand(MINIMUM_SUBMISSION_OPTION);
//        minimumSubmissionButton.addActionListener(submissionOptionListener);
//        buttonPanel.add(minimumSubmissionButton);
        buttonPanel.add(new JPanel());
        buttonGroup.add(minimumSubmissionButton);


        return buttonPanel;
    }

    /**
     * Panel contains the external links
     */
    private JPanel createOptionLinkPanel() {
        JPanel linkPanel = new JPanel();
        linkPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        // resubmission dialog
        resubmissionDialog = new ResubmissionDialog(app.getMainFrame());
        resubmissionDialog.setLocationRelativeTo(app.getMainFrame());
        resubmissionDialog.setVisible(false);

        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("resubmission.button.small.icon"));
        JButton resubmissionButton = GUIUtilities.createLabelLikeButton(icon, appContext.getProperty("resubmission.button.label"));
        resubmissionButton.setForeground(ColourUtil.HYPERLINK_COLOUR);
        resubmissionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resubmissionDialog.updateState();
                resubmissionDialog.setVisible(true);
            }
        });

        linkPanel.add(resubmissionButton);

        // bulk submission
        JButton loadSubmissionFileButton = GUIUtilities.createLabelLikeButton(new LoadSubmissionFileAction());
        loadSubmissionFileButton.setForeground(ColourUtil.HYPERLINK_COLOUR);
        linkPanel.add(loadSubmissionFileButton);

        // submission guideline
        icon = GUIUtilities.loadIcon(appContext.getProperty("welcome.submission.tool.link.small.icon"));
        JButton dataButton = GUIUtilities.createLabelLikeButton(icon, appContext.getProperty("welcome.submission.tool.submission.guideline"));
        dataButton.setForeground(ColourUtil.HYPERLINK_COLOUR);
        dataButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                HttpUtil.openURL(appContext.getProperty("px.submission.guideline.web.url"));
            }
        });
        linkPanel.add(dataButton);

        // more about proteomexchagne
        JButton moreButton = GUIUtilities.createLabelLikeButton(icon, appContext.getProperty("welcome.submission.tool.more.detail"));
        moreButton.setForeground(ColourUtil.HYPERLINK_COLOUR);
        moreButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                HttpUtil.openURL(appContext.getProperty("px.web.url"));
            }
        });
        linkPanel.add(moreButton);

        return linkPanel;
    }

    /**
     * Create a button represent a submission option
     */
    private JButton createOptionButton(String title, Icon icon) {
        JButton optionButton = new JButton();
        optionButton.setPreferredSize(new Dimension(240, 180));
        optionButton.setHorizontalTextPosition(JButton.CENTER);
        optionButton.setVerticalTextPosition(JButton.TOP);
        optionButton.setText(title);
        optionButton.setFont(optionButton.getFont().deriveFont(16f).deriveFont(Font.BOLD));
        optionButton.setIcon(icon);

        return optionButton;
    }

    private JPanel createRequirementPanel() {
        JPanel reqPanel = new JPanel(new BorderLayout());
        reqPanel.setBorder(BorderUtil.createLoweredBorder());

        // title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel(App.getInstance().getDesktopContext().getProperty("welcome.you.need.title"));
        titleLabel.setFont(titlePanel.getFont().deriveFont(16f).deriveFont(Font.BOLD));
        titlePanel.add(titleLabel, BorderLayout.NORTH);
//        JLabel descPanel = new JLabel(App.getInstance().getDesktopContext().getProperty("welcome.you.need.desc"));
//        titlePanel.add(descPanel, BorderLayout.CENTER);
        titlePanel.add(Box.createRigidArea(new Dimension(10, 10)), BorderLayout.SOUTH);
        reqPanel.add(titlePanel, BorderLayout.NORTH);

        requirementItemContainer = new JPanel();
        GridLayout layout = new GridLayout(1, 3);
        layout.setHgap(10);
        requirementItemContainer.setLayout(layout);

        reqPanel.add(requirementItemContainer, BorderLayout.CENTER);

        return reqPanel;
    }

    /**
     * Requirement panel for result file
     */
    private JPanel createResultFilePanel() {
        String header = appContext.getProperty("welcome.result.file.title");
        String subHeader = appContext.getProperty("welcome.result.file.desc");
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("welcome.result.file.small.icon"));
        return createRequiredItemPanel(header, subHeader, icon);
    }

    /**
     * Requirement panel for identification file
     */
    private JPanel createIdentificationFilePanel() {
        String header = appContext.getProperty("welcome.identification.file.title");
        String subHeader = appContext.getProperty("welcome.identification.file.desc");
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("welcome.identification.file.small.icon"));
        return createRequiredItemPanel(header, subHeader, icon);
    }

    /**
     * Requirement panel for raw file
     */
    private JPanel createRawFilePanel() {
        String header = appContext.getProperty("welcome.raw.file.title");
        String subHeader = appContext.getProperty("welcome.raw.file.desc");
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("welcome.raw.file.small.icon"));
        return createRequiredItemPanel(header, subHeader, icon);
    }

    /**
     * Requirement panel for pride login
     */
    private JPanel createPrideLoginPanel() {
        String header = appContext.getProperty("welcome.pride.login.title");
        String subHeader = appContext.getProperty("welcome.pride.login.desc");
        String hyperlinkSubHeader = appContext.getProperty("welcome.pride.login.hyperlink.desc");
        String hyperlinkURL = appContext.getProperty("pride.new.account.url");
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("welcome.pride.login.small.icon"));

        return createHyperlinkedRequiredItemPanel(header, subHeader, hyperlinkSubHeader, hyperlinkURL, icon);
    }

    /**
     * Generic requirement panel with hyperlinked sub titles
     */
    private JPanel createHyperlinkedRequiredItemPanel(String header, String subHeader, String hyperlinkedSubHeader, final String url, Icon icon) {
        RoundedPanel requirementPanel = new GradientRoundedPanel();
        requirementPanel.setShady(true);
        requirementPanel.setShadowColour(Color.gray);
        requirementPanel.setBorderColour(Color.gray);
        requirementPanel.setShadowGap(3);
        requirementPanel.setBackground(Color.white);
        requirementPanel.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        // large header
        c.gridx = 0;
        c.gridy = 1;

        JLabel largeHeaderLabel = new JLabel(header);
        largeHeaderLabel.setFont(largeHeaderLabel.getFont().deriveFont(16f).deriveFont(Font.BOLD));
        largeHeaderLabel.setHorizontalAlignment(JLabel.CENTER);
        contentPanel.add(largeHeaderLabel, c);

        // small header
        c.gridx = 0;
        c.gridy = 2;

        JPanel smallHeaderPanel = new JPanel();
        smallHeaderPanel.setOpaque(false);
        smallHeaderPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel smallHeaderLabel = new JLabel(subHeader);
        smallHeaderLabel.setHorizontalAlignment(JLabel.RIGHT);
        smallHeaderPanel.add(smallHeaderLabel);
        JLabel hyperlinkSmallHeaderLabel = new JLabel(hyperlinkedSubHeader);
        hyperlinkSmallHeaderLabel.setForeground(ColourUtil.HYPERLINK_COLOUR);
        hyperlinkSmallHeaderLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                HttpUtil.openURL(url);
            }
        });
        smallHeaderPanel.add(hyperlinkSmallHeaderLabel);
        contentPanel.add(smallHeaderPanel, c);

        // gap
        c.gridx = 0;
        c.gridy = 3;
        contentPanel.add(Box.createRigidArea(new Dimension(5, 5)), c);

        // icon
        c.gridx = 0;
        c.gridy = 4;

        JLabel iconLabel = new JLabel(icon);
        contentPanel.add(iconLabel, c);

        requirementPanel.add(contentPanel, BorderLayout.CENTER);

        return requirementPanel;
    }

    /**
     * Generic requirement panel
     */
    private JPanel createRequiredItemPanel(String header, String subHeader, Icon icon) {
        RoundedPanel requirementPanel = new GradientRoundedPanel();
        requirementPanel.setShady(true);
        requirementPanel.setShadowColour(Color.gray);
        requirementPanel.setBorderColour(Color.gray);
        requirementPanel.setShadowGap(3);
        requirementPanel.setBackground(Color.white);
        requirementPanel.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        // large header
        c.gridx = 0;
        c.gridy = 1;

        JLabel largeHeaderLabel = new JLabel(header);
        largeHeaderLabel.setFont(largeHeaderLabel.getFont().deriveFont(16f).deriveFont(Font.BOLD));
        largeHeaderLabel.setHorizontalAlignment(JLabel.CENTER);
        contentPanel.add(largeHeaderLabel, c);

        // small header
        c.gridx = 0;
        c.gridy = 2;

        JLabel smallHeaderLabel = new JLabel(subHeader);
        smallHeaderLabel.setHorizontalAlignment(JLabel.CENTER);
        contentPanel.add(smallHeaderLabel, c);

        // gap
        c.gridx = 0;
        c.gridy = 3;
        contentPanel.add(Box.createRigidArea(new Dimension(5, 5)), c);

        // icon
        c.gridx = 0;
        c.gridy = 4;

        JLabel iconLabel = new JLabel(icon);
        contentPanel.add(iconLabel, c);

        requirementPanel.add(contentPanel, BorderLayout.CENTER);

        return requirementPanel;
    }

    /**
     * Listener to perform actions when a submission option is selected
     */
    private class SubmissionOptionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals(FULL_SUBMISSION_OPTION)) {
                // set submission option in application context
                appContext.setSubmissionsType(SubmissionType.COMPLETE);
                // change the colour of the option button
                fullSubmissionButton.setForeground(Color.BLACK);
                partialSubmissionButton.setForeground(Color.GRAY);
                fullSubmissionButton.setIcon(GUIUtilities.loadIcon(appContext.getProperty("welcome.full.submission.button.title.selected.large.icon")));
                partialSubmissionButton.setIcon(GUIUtilities.loadIcon(appContext.getProperty("welcome.partial.submission.button.title.large.icon")));
//                minimumSubmissionButton.setIcon(GUIUtilities.loadIcon(appContext.getProperty("welcome.minimum.submission.button.title.large.icon")));
                // change the required information
                requirementItemContainer.removeAll();
                requirementItemContainer.add(resultFileRequirementItemPanel);
                requirementItemContainer.add(rawFileRequirementItemPanel);
                requirementItemContainer.add(prideLoginRequirementItemPanel);
            } else if (e.getActionCommand().equals(PARTIAL_SUBMISSION_OPTION)) {
                // set submission option in application context
                appContext.setSubmissionsType(SubmissionType.PARTIAL);

                // change the colour of the option button
                fullSubmissionButton.setForeground(Color.GRAY);
                partialSubmissionButton.setForeground(Color.BLACK);
                fullSubmissionButton.setIcon(GUIUtilities.loadIcon(appContext.getProperty("welcome.full.submission.button.title.large.icon")));
                partialSubmissionButton.setIcon(GUIUtilities.loadIcon(appContext.getProperty("welcome.partial.submission.button.title.selected.large.icon")));
//                minimumSubmissionButton.setIcon(GUIUtilities.loadIcon(appContext.getProperty("welcome.minimum.submission.button.title.large.icon")));
                // change the required information
                requirementItemContainer.removeAll();
                requirementItemContainer.add(identificationFileRequirementItemPanel);
                requirementItemContainer.add(rawFileRequirementItemPanel);
                requirementItemContainer.add(prideLoginRequirementItemPanel);
            }

            requirementItemContainer.revalidate();
            requirementItemContainer.repaint();
        }
    }

}
