package uk.ac.ebi.pride;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.gui.desktop.Desktop;
import uk.ac.ebi.pride.gui.desktop.DesktopContext;
import uk.ac.ebi.pride.gui.form.*;
import uk.ac.ebi.pride.gui.navigation.NavigationException;
import uk.ac.ebi.pride.gui.navigation.NavigationPanelDescriptor;
import uk.ac.ebi.pride.gui.navigation.Navigator;
import uk.ac.ebi.pride.gui.util.Constant;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

/**
 * This is the entry point of the entire application
 * <p/>
 * This class is responsible to start the application and show GUI components
 */
public class App extends Desktop {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    /**
     * Main frame of the application
     */
    private JFrame mainFrame;

    /**
     * Navigator panel
     */
    private Navigator navigator;

    public static void main(String[] args) {
        Desktop.launch(App.class, AppContext.class, args);
    }

    @Override
    public void init(String[] args) {
        // load properties
        loadProps();

        // init key controls
        initKeyControls();

        // init user space
        initUserSpace();

        // build main frame
        buildMainFrame();

        // build navigation
        buildNavigation();
    }

    /**
     * Load property settings before showing the GUI
     */
    private void loadProps() {
        DesktopContext context = getDesktopContext();
        try {
            context.loadSystemProps(this.getClass().getClassLoader().getResourceAsStream("prop/gui.prop"));
            context.loadSystemProps(this.getClass().getClassLoader().getResourceAsStream("prop/setting.prop"));
        } catch (IOException e) {
            logger.error("Error while loading properties", e);
        }
    }

    /**
     * Initialize key controls for mac platform
     */
    private void initKeyControls() {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Mac OS")) {
            InputMap textFieldInputMap = (InputMap) UIManager.get("TextField.focusInputMap");
            textFieldInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), DefaultEditorKit.copyAction);
            textFieldInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), DefaultEditorKit.pasteAction);
            textFieldInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), DefaultEditorKit.cutAction);

            InputMap textAreaInputMap = (InputMap) UIManager.get("TextArea.focusInputMap");
            textAreaInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), DefaultEditorKit.copyAction);
            textAreaInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), DefaultEditorKit.pasteAction);
            textAreaInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), DefaultEditorKit.cutAction);

            InputMap passwordFieldInputMap = (InputMap) UIManager.get("PasswordField.focusInputMap");
            passwordFieldInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), DefaultEditorKit.copyAction);
            passwordFieldInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), DefaultEditorKit.pasteAction);
            passwordFieldInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), DefaultEditorKit.cutAction);
        }
    }

    /**
     * Initialize a space for saving user related details
     * such as: submission progress report
     */
    private void initUserSpace() {
        File userSpace = new File(System.getProperty("user.home") + File.separator + Constant.PX_TOOL_USER_DIRECTORY);
        if (!userSpace.exists()) {
            userSpace.mkdir();
        }
    }

    /**
     * Build the main display frame and set the look and feel
     */
    private void buildMainFrame() {
        mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // set look and feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }

        } catch (Exception e) {
            logger.error("Failed to load nimbus look and feel for the application", e);
        }
    }

    /**
     * Build the main navigation panel
     */
    private void buildNavigation() {
        navigator = new Navigator();

        try {
            // register welcome form
            WelcomeDescriptor welcomePanel = new WelcomeDescriptor(getDesktopContext().getProperty("welcome.nav.desc.id"),
                    getDesktopContext().getProperty("welcome.nav.desc.title"),
                    getDesktopContext().getProperty("welcome.nav.desc.detail") +
                            " (version " + getDesktopContext().getProperty("px.submission.tool.version") + ")");
            navigator.registerNavigationPanel(welcomePanel);

            // login form
            PrideLoginDescriptor prideLoginPanel = new PrideLoginDescriptor(getDesktopContext().getProperty("pride.login.nav.desc.id"),
                    getDesktopContext().getProperty("pride.login.nav.desc.title"),
                    getDesktopContext().getProperty("pride.login.nav.desc.detail"));
            navigator.registerNavigationPanel(prideLoginPanel);

            // register project metadata form
            ProjectMetaDataDescriptor metaDataPanel = new ProjectMetaDataDescriptor(getDesktopContext().getProperty("project.metadata.nav.desc.id"),
                    getDesktopContext().getProperty("project.metadata.nav.desc.title"),
                    getDesktopContext().getProperty("project.metadata.nav.desc.detail"));
            navigator.registerNavigationPanel(metaDataPanel);

            // register file selection form
            FileSelectionDescriptor fileSelectionPanel = new FileSelectionDescriptor(getDesktopContext().getProperty("file.selection.nav.desc.id"),
                    getDesktopContext().getProperty("file.selection.nav.desc.title"),
                    getDesktopContext().getProperty("file.selection.nav.desc.detail"));
            navigator.registerNavigationPanel(fileSelectionPanel);

            // register file mapping form
            FileMappingDescriptor fileMappingPanel = new FileMappingDescriptor(getDesktopContext().getProperty("file.mapping.nav.desc.id"),
                    getDesktopContext().getProperty("file.mapping.nav.desc.title"),
                    getDesktopContext().getProperty("file.mapping.nav.desc.detail"));
            navigator.registerNavigationPanel(fileMappingPanel);

            // register sample metadata form
            SampleMetaDataDescriptor sampleMetaDataPanel = new SampleMetaDataDescriptor(getDesktopContext().getProperty("sample.metadata.nav.desc.id"),
                    getDesktopContext().getProperty("sample.metadata.nav.desc.title"),
                    getDesktopContext().getProperty("sample.metadata.nav.desc.detail"));
            navigator.registerNavigationPanel(sampleMetaDataPanel);

            // register additional metadata form
            AdditionalMetaDataDescriptor additionalMetaDataPanel = new AdditionalMetaDataDescriptor(getDesktopContext().getProperty("additional.metadata.nav.desc.id"),
                    getDesktopContext().getProperty("additional.metadata.nav.desc.title"),
                    getDesktopContext().getProperty("additional.metadata.nav.desc.detail"));
            navigator.registerNavigationPanel(additionalMetaDataPanel);

            // lab head form
            LabHeadDescriptor labHeadPanel = new LabHeadDescriptor(getDesktopContext().getProperty("pride.labhead.nav.desc.id"),
                    getDesktopContext().getProperty("pride.labhead.nav.desc.title"),
                    getDesktopContext().getProperty("pride.labhead.nav.desc.detail"));
            navigator.registerNavigationPanel(labHeadPanel);

            // register additional project metadata form
            AdditionalDatasetDetailsDescriptor projectTagPanel = new AdditionalDatasetDetailsDescriptor(getDesktopContext().getProperty("additional.project.metadata.nav.desc.id"),
                    getDesktopContext().getProperty("additional.project.metadata.nav.desc.title"),
                    getDesktopContext().getProperty("additional.project.metadata.nav.desc.detail"));
            navigator.registerNavigationPanel(projectTagPanel);

            // register submission summary
            SummaryDescriptor summaryPanel = new SummaryDescriptor(getDesktopContext().getProperty("summary.nav.desc.id"),
                    getDesktopContext().getProperty("summary.nav.desc.title"),
                    getDesktopContext().getProperty("summary.nav.desc.detail"));
            navigator.registerNavigationPanel(summaryPanel);

            // register submission form
            SubmissionDescriptor submissionPanel = new SubmissionDescriptor(getDesktopContext().getProperty("submission.nav.desc.id"),
                    getDesktopContext().getProperty("submission.nav.desc.title"),
                    getDesktopContext().getProperty("submission.nav.desc.detail"));
            navigator.registerNavigationPanel(submissionPanel);

            // register finish form
            navigator.registerNavigationPanel(NavigationPanelDescriptor.FINISH);

            // set the first form
            navigator.setCurrentNavigationPanel(welcomePanel.getNavigationPanelId());

        } catch (NavigationException e) {
            logger.error("Failed to register navigation panels", e);
            // todo: system exist
        }

        mainFrame.setContentPane(navigator);
    }

    /**
     * Get the main display frame
     *
     * @return JFrame  main display frame
     */
    public JFrame getMainFrame() {
        return mainFrame;
    }

    /**
     * Get the navigator
     *
     * @return
     */
    public Navigator getNavigator() {
        return navigator;
    }

    @Override
    public void show() {
        DesktopContext context = getDesktopContext();
        int defaultWidth = Integer.parseInt(context.getProperty("px.submission.tool.width"));
        int defaultHeight = Integer.parseInt(context.getProperty("px.submission.tool.height"));
        mainFrame.setSize(defaultWidth, defaultHeight);

        // set display location
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        mainFrame.setLocation((d.width - mainFrame.getWidth()) / 2, (d.height - mainFrame.getHeight()) / 2);
        mainFrame.setVisible(true);
    }

    @Override
    public void ready() {
    }

    @Override
    public void postShow() {
    }

    @Override
    public void finish() {
        mainFrame.dispose();
    }

    /**
     * Restart the submission process
     */
    public void restart() {
        buildNavigation();
        mainFrame.validate();
        mainFrame.repaint();
    }
}
