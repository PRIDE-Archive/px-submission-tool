package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.archive.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.toolsuite.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.form.comp.GradientRoundedPanel;
import uk.ac.ebi.pride.gui.form.comp.RoundedPanel;
import uk.ac.ebi.pride.gui.util.BorderUtil;
import uk.ac.ebi.pride.gui.util.ColourUtil;
import uk.ac.ebi.pride.gui.util.HttpUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * This form shows welcome messages
 *
 * @author Rui Wang
 * @version $Id$
 */
public class PrerequisiteForm extends Form {

    private JPanel requirementItemContainer;
    private JPanel resultFileRequirementItemPanel;
    private JPanel identificationFileRequirementItemPanel;
    private JPanel rawFileRequirementItemPanel;
    private JPanel prideLoginRequirementItemPanel;
    private JPanel experimentMetadataItemPanel;
    private JPanel labHeadItemPanel;

    public PrerequisiteForm() {
        initComponents();
    }

    private void initComponents() {
        // setup the main pane
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        // create requirement panel
        JPanel detailPanel = createRequirementPanel();
        this.add(detailPanel, BorderLayout.CENTER);

        // create requirement item panel
        resultFileRequirementItemPanel = createResultFilePanel();
        identificationFileRequirementItemPanel = createIdentificationFilePanel();
        rawFileRequirementItemPanel = createRawFilePanel();
        prideLoginRequirementItemPanel = createPrideLoginPanel();
        experimentMetadataItemPanel = createExperimentMetadataPanel();
        labHeadItemPanel = createLabHeadPanel();
    }

    private JPanel createRequirementPanel() {
        JPanel reqPanel = new JPanel(new BorderLayout());
        reqPanel.setBorder(BorderUtil.createLoweredBorder());

        // title panel
//        JPanel titlePanel = new JPanel(new BorderLayout());
//        JLabel titleLabel = new JLabel(App.getInstance().getDesktopContext().getProperty("welcome.you.need.title"));
//        titleLabel.setFont(titlePanel.getFont().deriveFont(16f).deriveFont(Font.BOLD));
//        titlePanel.add(titleLabel, BorderLayout.NORTH);
////        JLabel descPanel = new JLabel(App.getInstance().getDesktopContext().getProperty("welcome.you.need.desc"));
////        titlePanel.add(descPanel, BorderLayout.CENTER);
//        titlePanel.add(Box.createRigidArea(new Dimension(10, 10)), BorderLayout.SOUTH);
//        reqPanel.add(titlePanel, BorderLayout.NORTH);

        requirementItemContainer = new JPanel();
        GridLayout layout = new GridLayout(2, 3);
        layout.setHgap(20);
        layout.setVgap(30);
        requirementItemContainer.setLayout(layout);

        reqPanel.add(requirementItemContainer, BorderLayout.CENTER);

        return reqPanel;
    }

    /**
     * Requirement panel for result file
     */
    private JPanel createResultFilePanel() {
        String header = appContext.getProperty("prerequisite.result.file.title");
        String subHeader = appContext.getProperty("prerequisite.result.file.desc");
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("prerequisite.result.file.small.icon"));
        return createRequiredItemPanel(header, subHeader, icon);
    }

    /**
     * Requirement panel for identification file
     */
    private JPanel createIdentificationFilePanel() {
        String header = appContext.getProperty("prerequisite.identification.file.title");
        String subHeader = appContext.getProperty("prerequisite.identification.file.desc");
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("prerequisite.identification.file.small.icon"));
        return createRequiredItemPanel(header, subHeader, icon);
    }

    /**
     * Requirement panel for raw file
     */
    private JPanel createRawFilePanel() {
        String header = appContext.getProperty("prerequisite.raw.file.title");
        String subHeader = appContext.getProperty("prerequisite.raw.file.desc");
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("prerequisite.raw.file.small.icon"));
        return createRequiredItemPanel(header, subHeader, icon);
    }

    /**
     * Requirement panel for pride login
     */
    private JPanel createPrideLoginPanel() {
        String header = appContext.getProperty("prerequisite.pride.login.title");
        String subHeader = appContext.getProperty("prerequisite.pride.login.desc");
        String hyperlinkSubHeader = appContext.getProperty("prerequisite.pride.login.hyperlink.desc");
        String hyperlinkURL = appContext.getProperty("pride.new.account.url");
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("prerequisite.pride.login.small.icon"));

        return createHyperlinkedRequiredItemPanel(header, subHeader, hyperlinkSubHeader, hyperlinkURL, icon);
    }

    /**
     * Requirement panel for experiment metadata
     */
    private JPanel createExperimentMetadataPanel() {
        String header = appContext.getProperty("prerequisite.metadata.title");
        String subHeader = appContext.getProperty("prerequisite.metadata.desc");
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("prerequisite.metadata.small.icon"));
        return createRequiredItemPanel(header, subHeader, icon);
    }

    /**
     * Requirement panel for lab head
     */
    private JPanel createLabHeadPanel() {
        String header = appContext.getProperty("prerequisite.labhead.title");
        String subHeader = appContext.getProperty("prerequisite.labhead.desc");
        Icon icon = GUIUtilities.loadIcon(appContext.getProperty("prerequisite.labhead.small.icon"));
        return createRequiredItemPanel(header, subHeader, icon);
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

    public void setSubmissionType(SubmissionType submissionType) {
        if (SubmissionType.COMPLETE.equals(submissionType)) {
            requirementItemContainer.removeAll();
            requirementItemContainer.add(resultFileRequirementItemPanel);
            requirementItemContainer.add(rawFileRequirementItemPanel);
            requirementItemContainer.add(prideLoginRequirementItemPanel);
            requirementItemContainer.add(experimentMetadataItemPanel);
            requirementItemContainer.add(labHeadItemPanel);
        } else if (SubmissionType.PARTIAL.equals(submissionType)){
            requirementItemContainer.removeAll();
            requirementItemContainer.add(identificationFileRequirementItemPanel);
            requirementItemContainer.add(rawFileRequirementItemPanel);
            requirementItemContainer.add(prideLoginRequirementItemPanel);
            requirementItemContainer.add(experimentMetadataItemPanel);
            requirementItemContainer.add(labHeadItemPanel);
        }

        requirementItemContainer.revalidate();
        requirementItemContainer.repaint();
    }


}
