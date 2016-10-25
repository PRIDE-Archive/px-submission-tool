package uk.ac.ebi.pride.gui.form.table.model;

import java.util.*;

/**
 * This class is the model for the project tag table. It reads a list of projects tags in from a resources file,
 * and then handles which tags have been added to the submission. In the case of HPP and B/D-HPP, these "main"
 * project tags are added if sub-project tag is added. NB all these sub-project tags are considered to be "parent"
 * tags as well within PRIDE.
 *
 * @author Tobias Ternent
 * date 2016-10-13
 */

public class ProjectTagTableModel extends PxTableModel {
    private static final String HUMAN_PROTEOME_PROJECT = "Human Proteome Project";
    private static final String HPP = "HPP";
    private static final String BIO_DISEASE_HPP = "Biology/Disease-Driven Human Proteome Project (B/D-HPP)";
    private static final String BD_HPP = "B/D-HPP";

    /**
     * This enum defines the header for the project tag table.
     */
    public enum TableHeader {
        SELECTION("", "Select a related project"),
        PROJECT_TAG("Parent Project", "Parent project name");

        private final String header;
        private final String toolTip;

        TableHeader(String header, String tooltip) {
            this.header = header;
            this.toolTip = tooltip;
        }

        public String getHeader() {
            return header;
        }

        public String getToolTip() {
            return toolTip;
        }
    }

    private final Map<String, Boolean> projectTags = new LinkedHashMap<>();

    /**
     * The constructor by default defines that no project tag have been selected yet.
     *
     * @param projectTags all the project tags that may be used.
     */
    public ProjectTagTableModel(Collection<String> projectTags) {
        super();
        for (String projectTag : projectTags) {
            this.projectTags.put(projectTag, false);
        }
    }

    /**
     * This method initialises the table.
     */
    @Override
    protected void initializeTableModel() {
        TableHeader[] headers = TableHeader.values();
        for (TableHeader header : headers) {
            columnNames.put(header.getHeader(), header.getToolTip());
        }
    }

    /**
     * This method gets the total number of project tags.
     *
     * @return the total mumber of project tags.
     */
    @Override
    public int getRowCount() {
        return projectTags.size();
    }

    /**
     * This method gets the current selected value.
     *
     * @param rowIndex the current row position.
     * @param columnIndex the current column position.
     * @return Null if there are both no row and column positions, or in this case the project tag as a String.
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (!projectTags.isEmpty() && rowIndex >= 0 && columnIndex >= 0) {
            List<Map.Entry<String, Boolean>> entries = new ArrayList<>(projectTags.entrySet());
            Map.Entry<String, Boolean> entry = entries.get(rowIndex);
            String projectTag = entry.getKey();
            Boolean selected = entry.getValue();
            if (TableHeader.SELECTION.getHeader().equals(getColumnName(columnIndex))) {
                return selected;
            } else if (TableHeader.PROJECT_TAG.getHeader().equals(getColumnName(columnIndex))) {
                return projectTag;
            }
        }
        return null;
    }

    /**
     * This method gets all the currently selected project tags.
     *
     * @return a set of all the currently selected project tags.
     */
    public Set<String> getSelectedProjectTags() {
        Set<String> selectedProjectTags = new HashSet<>();
        for (String projectTag : projectTags.keySet()) {
            if (projectTags.get(projectTag)) {
                selectedProjectTags.add(projectTag);
            }
        }

        return selectedProjectTags;
    }

    /**
     * This method sets all the currently selected project tags.
     */
    public void setSelectedProjectTags(Collection<String> selectedProjectTags) {
        for (String projectTag : projectTags.keySet()) {
            projectTags.put(projectTag, selectedProjectTags.contains(projectTag));
        }
        fireTableDataChanged();
    }

    /**
     * This method checks to see if the cell is editable or not.
     *
     * @param rowIndex the current row position.
     * @param columnIndex the current column position.
     * @return true if editable, false otherwise.
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return TableHeader.SELECTION.getHeader().equals(getColumnName(columnIndex)) || super.isCellEditable(rowIndex, columnIndex);
    }

    /**
     * This method gets the column's class.
     *
     * @param columnIndex the current column position.
     * @return the class of the column.
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (TableHeader.SELECTION.getHeader().equals(getColumnName(columnIndex))) {
            return Boolean.class;
        } else {
            return super.getColumnClass(columnIndex);
        }
    }

    /**
     * This method selects or deselects a project tag. HPP and B/D=HPP tags are automatically selected
     * when one of their sub-projects are selected.
     *
     * @param aValue True if the tag is selected, False otherwise.
     * @param rowIndex the current row position.
     * @param columnIndex the current column position.
     */
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        List<Map.Entry<String, Boolean>> entries = new ArrayList<>(projectTags.entrySet());
        Map.Entry<String, Boolean> entry = entries.get(rowIndex);
        String projecTag = entry.getKey();
        if (TableHeader.SELECTION.getHeader().equals(getColumnName(columnIndex))) {
            Boolean selection = (Boolean) aValue;
            projectTags.put(projecTag, selection);
            if ((projecTag.contains(HUMAN_PROTEOME_PROJECT) || projecTag.contains(HPP)) && selection) {
                projectTags.put(HUMAN_PROTEOME_PROJECT, true);
            } // automatically set HPP tag
            if (projecTag.contains(BD_HPP) && selection) {
                projectTags.put(BIO_DISEASE_HPP, true);
            } // automatically set B/D-HPP tag
            fireTableDataChanged();
        } else {
            super.setValueAt(aValue, rowIndex, columnIndex);
        }
    }
}
