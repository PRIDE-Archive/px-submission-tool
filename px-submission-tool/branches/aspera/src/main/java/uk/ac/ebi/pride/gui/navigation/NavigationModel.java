package uk.ac.ebi.pride.gui.navigation;

import uk.ac.ebi.pride.gui.prop.PropertyChangeHelper;

import java.util.*;

/**
 * NavigationModel is responsible for storing and managing all the navigation panel descriptor
 * <p/>
 * todo: create unit tests
 *
 * @author Rui Wang
 * @version $Id$
 */
public class NavigationModel extends PropertyChangeHelper {
    public static final String CURRENT_NAVIGATION_PANEL_CHANGE_PROPERTY = "currentNavigationPanelChange";

    /**
     * Map stores all the navigation panel
     */
    private Map<Object, NavigationPanelDescriptor> descriptors;

    /**
     * Current visible navigation panel
     */
    private NavigationPanelDescriptor currentPanel;

    public NavigationModel() {
        descriptors = new LinkedHashMap<Object, NavigationPanelDescriptor>();
    }

    /**
     * Get a navigation panel descriptor using a given id
     *
     * @param id panel descriptor id
     * @return NavigationPanelDescriptor   navigation panel descriptor
     */
    public NavigationPanelDescriptor getPanelDescriptor(Object id) {
        return descriptors.get(id);
    }

    /**
     * Get current navigation panel
     *
     * @return NavigationPanelDescriptor   current navigation panel
     */
    public NavigationPanelDescriptor getCurrentPanelDescriptor() {
        return currentPanel;
    }

    /**
     * Check a given panel descriptor id is pointing to back panel
     *
     * @param id given panel descriptor id
     * @return boolean true means back panel
     */
    public boolean isBackPanelDescriptor(Object id) {
        boolean backPanel = false;

        // index of current panel
        int currIndex = getIndexOfCurrentNavigationPanel();

        // index of next panel
        if (currIndex > 0) {
            List<Object> ids = new ArrayList<Object>(descriptors.keySet());
            Object backPanelId = null;
            for (int i = (currIndex - 1); i >= 0; i--) {
                if (!descriptors.get(ids.get(i)).toSkipPanel()) {
                    backPanelId = ids.get(i);
                    break;
                }
            }
            backPanel = id.equals(backPanelId);
        }


        return backPanel;
    }

    /**
     * Get previous navigation panel
     *
     * @return NavigationPanelDescriptor   previous navigation panel
     */
    public NavigationPanelDescriptor getBackPanelDescriptor() {
        NavigationPanelDescriptor previousPanel = null;

        // index of current panel
        int currIndex = getIndexOfCurrentNavigationPanel();

        // index of next panel
        if (currIndex > 0) {
            List<Object> ids = new ArrayList<Object>(descriptors.keySet());
            for (int i = (currIndex - 1); i >= 0; i--) {
                if (!descriptors.get(ids.get(i)).toSkipPanel()) {
                    previousPanel = descriptors.get(ids.get(i));
                    break;
                }
            }
        }

        return previousPanel;
    }

    /**
     * Check a given panel descriptor id is pointing to next panel
     *
     * @param id given panel descriptor id
     * @return boolean true means next panel
     */
    public boolean isNextPanelDescriptor(Object id) {
        boolean isNextPanel = false;

        // index of current panel
        int currIndex = getIndexOfCurrentNavigationPanel();

        // index of next panel
        if (currIndex >= 0 && currIndex < descriptors.size() - 1) {
            List<Object> ids = new ArrayList<Object>(descriptors.keySet());
            Object nextPanelId = null;
            for (int i = (currIndex + 1); i < ids.size(); i++) {
                if (!descriptors.get(ids.get(i)).toSkipPanel()) {
                    nextPanelId = ids.get(i);
                    break;
                }
            }
            isNextPanel = id.equals(nextPanelId);
        }

        return isNextPanel;
    }

    /**
     * Get next navigation panel
     *
     * @return NavigationPanelDescriptor   next navigation panel
     */
    public NavigationPanelDescriptor getNextPanelDescriptor() {
        NavigationPanelDescriptor nextPanel = null;

        // index of current panel
        int currIndex = getIndexOfCurrentNavigationPanel();

        // index of next panel
        if (currIndex >= 0 && currIndex < descriptors.size() - 1) {
            List<Object> ids = new ArrayList<Object>(descriptors.keySet());
            for (int i = (currIndex + 1); i < ids.size(); i++) {
                if (!descriptors.get(ids.get(i)).toSkipPanel()) {
                    nextPanel = descriptors.get(ids.get(i));
                    break;
                }
            }
        }

        return nextPanel;
    }

    /**
     * Register a new navigation panel descriptor
     *
     * @param descriptor navigation panel descriptor
     * @throws NavigationException exception while registering a new navigation panel
     */
    public void registerNavigationPanel(NavigationPanelDescriptor descriptor) throws NavigationException {
        if (descriptor == null) {
            throw new NavigationException("Failed to register a new navigation panel, NavigationPanelDescriptor: " + descriptor);
        }
        descriptors.put(descriptor.getNavigationPanelId(), descriptor);
    }

    /**
     * Check whether there is a existing current navigation panel descriptor
     *
     * @return boolean true means there is a current navigation panel descriptor
     */
    public boolean hasCurrentNavigationPanel() {
        return currentPanel != null;
    }

    /**
     * Set a new panel to be the current visible navigation panel
     *
     * @param id navigation panel id
     * @throws NavigationException error while setting current navigation panel
     */
    public void setCurrentNavigationPanel(Object id) throws NavigationException {
        NavigationPanelDescriptor newPanel = descriptors.get(id);

        if (newPanel == null) {
            throw new NavigationException("Navigation panel not found");
        }

        NavigationPanelDescriptor oldPanel = currentPanel;
        currentPanel = newPanel;

        firePropertyChange(CURRENT_NAVIGATION_PANEL_CHANGE_PROPERTY, oldPanel, newPanel);
    }

    /**
     * Return index of the current navigation panel
     *
     * @return int index of the current navigation panel
     */
    private int getIndexOfCurrentNavigationPanel() {
        int currIndex = -1;

        if (currentPanel != null) {
            Object currId = currentPanel.getNavigationPanelId();
            List<Object> ids = new ArrayList<Object>(descriptors.keySet());
            for (int i = 0; i < ids.size(); i++) {
                if (ids.get(i).equals(currId)) {
                    currIndex = i;
                    break;
                }
            }
        }

        return currIndex;
    }
}
